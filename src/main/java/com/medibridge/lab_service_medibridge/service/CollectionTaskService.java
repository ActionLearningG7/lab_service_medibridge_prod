package com.medibridge.lab_service_medibridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.SampleManifest;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus;
import com.medibridge.lab_service_medibridge.domain.model.SampleCollectionRequest;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.domain.repository.SampleManifestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionTaskService {

    private final CollectionTaskRepository taskRepository;
    private final LabOrderRepository labOrderRepository;
    private final SampleManifestRepository sampleManifestRepository;
    private final RedisLockService lockService;
    private final TaskStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final com.medibridge.lab_service_medibridge.event.DomainEventPublisher eventPublisher;

    // Kafka producer could be injected here for events

    /**
     * Create initial task from order (Internal Use)
     */
    @Transactional
    public void createTaskFromOrder(LabOrder order) {
        if (taskRepository.findByLabOrderId(order.getId()).isPresent()) {
            return; // Idempotent
        }

        CollectionTask task = CollectionTask.builder()
                .labOrderId(order.getId())
                .status(CollectionTaskStatus.CREATED)
                .requestedSlot(order.getPreferredSlotStart())
                .scheduledAt(order.getPreferredSlotStart())
                .priority(0)
                .createdAt(LocalDateTime.now())
                .build();

        taskRepository.save(task);
        log.info("Created CollectionTask for Order {}", order.getOrderNumber());
        eventPublisher.publishTaskCreated(task.getId(), order.getId());
    }

    /**
     * Admin assignment
     */
    @Transactional
    public void assignTask(UUID taskId, String phlebotomistId, LocalDateTime scheduledAt, String notes,
            String adminId) {
        String lockKey = taskId.toString();
        if (!lockService.acquireLock(lockKey)) {
            throw new IllegalStateException("Task is currently locked by another process");
        }

        try {
            CollectionTask task = getTask(taskId);
            stateMachine.validateTransition(task.getStatus(), CollectionTaskStatus.ASSIGNED);

            CollectionTaskStatus oldStatus = task.getStatus();
            task.setStatus(CollectionTaskStatus.ASSIGNED);
            task.setPhlebotomistId(phlebotomistId);
            if (scheduledAt != null) {
                task.setScheduledAt(scheduledAt);
            }
            if (notes != null) {
                task.setNotesForPhlebotomist(notes);
            }
            task.setAssignedAt(LocalDateTime.now());

            taskRepository.save(task);
            updateOrder(task.getLabOrderId(), LabOrderStatus.ASSIGNED);

            eventPublisher.publishStatusChanged(taskId, oldStatus, CollectionTaskStatus.ASSIGNED);
            eventPublisher.publishTaskAssigned(taskId, phlebotomistId);

            log.info("Task {} assigned to {} by {}", taskId, phlebotomistId, adminId);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }

    /**
     * Phlebotomist actions: ACCEPT, EN_ROUTE, ARRIVE, DELIVER, COMPLETE
     */
    @Transactional
    public void transition(UUID taskId, CollectionTaskStatus nextStatus, String actorId) {
        String lockKey = taskId.toString();
        if (!lockService.acquireLock(lockKey)) {
            throw new IllegalStateException("Task is locked");
        }

        try {
            CollectionTask task = getTask(taskId);

            // Validate ownership
            if (!task.getPhlebotomistId().equals(actorId)) {
                throw new SecurityException("You are not assigned to this task");
            }

            stateMachine.validateTransition(task.getStatus(), nextStatus);
            CollectionTaskStatus oldStatus = task.getStatus();

            // Apply logic
            task.setStatus(nextStatus);
            LocalDateTime now = LocalDateTime.now();

            switch (nextStatus) {
                case ACCEPTED -> task.setAcceptedAt(now);
                case EN_ROUTE -> task.setEnRouteAt(now);
                case ARRIVED -> {
                    task.setArrivedAt(now);
                    // Order status update
                    // In some flows, order might stay ASSIGNED until collection, or move to
                    // IN_COLLECTION
                }
                case DELIVERED_TO_LAB -> {
                    task.setDeliveredAt(now);
                    updateOrder(task.getLabOrderId(), LabOrderStatus.IN_TRANSIT);
                }
                case COMPLETED -> {
                    task.setCompletedAt(now);
                    // Order logic usually handles AT_LAB -> TESTING -> COMPLETED
                }
                default -> {
                }
            }

            taskRepository.save(task);
            eventPublisher.publishStatusChanged(taskId, oldStatus, nextStatus);
            log.info("Task {} transitioned to {} by {}", taskId, nextStatus, actorId);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }

    /**
     * Phlebotomist action: Collect Samples
     */
    @Transactional
    public void collectSamples(UUID taskId, SampleCollectionRequest request, String phlebId) {
        // Lock
        String lockKey = taskId.toString();
        if (!lockService.acquireLock(lockKey))
            throw new IllegalStateException("Locked");

        try {
            CollectionTask task = getTask(taskId);
            if (!task.getPhlebotomistId().equals(phlebId))
                throw new SecurityException("Unauthorized");

            stateMachine.validateTransition(task.getStatus(), CollectionTaskStatus.SAMPLES_COLLECTED);

            // Save Manifest
            String sampleJson;
            try {
                sampleJson = objectMapper.writeValueAsString(request.getSamples());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid sample data");
            }

            SampleManifest manifest = SampleManifest.builder()
                    .taskId(taskId)
                    .sampleData(sampleJson)
                    .pickupVerifiedByOtp(request.isOtpVerified())
                    .verifiedAt(request.isOtpVerified() ? LocalDateTime.now() : null)
                    .collectedBy(phlebId)
                    .collectedAt(LocalDateTime.now())
                    .temperatureAtCollection(request.getTemperatureCelsius())
                    .createdAt(LocalDateTime.now())
                    .build();
            sampleManifestRepository.save(manifest);

            // Update Task
            task.setStatus(CollectionTaskStatus.SAMPLES_COLLECTED);
            task.setCollectedAt(LocalDateTime.now());
            taskRepository.save(task);

            // Update Order
            updateOrder(task.getLabOrderId(), LabOrderStatus.COLLECTED);

        } finally {
            lockService.releaseLock(lockKey);
        }
    }

    private CollectionTask getTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }

    // Internal update helper
    private void updateOrder(UUID orderId, LabOrderStatus newStatus) {
        LabOrder order = labOrderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(newStatus);
            labOrderRepository.save(order);
        }
    }
}
