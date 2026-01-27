package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.*;
import com.medibridge.lab_service_medibridge.domain.enums.*;
import com.medibridge.lab_service_medibridge.event.KafkaProducer;
import com.medibridge.lab_service_medibridge.repository.*;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionTaskRepository taskRepository;
    private final LabOrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    // private final LabTestCatalogRepository catalogRepository; // Removed
    private final AuditService auditService;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public void assignTask(String taskId, String phlebotomistId) {
        CollectionTask task = taskRepository.findById(UUID.fromString(taskId))
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        task.setPhlebotomistId(phlebotomistId);
        task.setStatus(CollectionTaskStatus.ASSIGNED);
        taskRepository.save(task);

        // Update Order
        LabOrder order = orderRepository.findById(task.getLabOrderId()).orElseThrow();
        order.setStatus(LabOrderStatus.ASSIGNED);
        orderRepository.save(order);

        auditService.logAction(AuditAction.ASSIGN, "CollectionTask", taskId, "Assigned to " + phlebotomistId);
        kafkaProducer.publish("collection-task-assigned", taskId);
    }

    @Transactional
    public void markCollected(String taskId, String notes) {
        CollectionTask task = taskRepository.findById(UUID.fromString(taskId))
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        task.setStatus(CollectionTaskStatus.SAMPLES_COLLECTED);
        task.setCollectedAt(LocalDateTime.now());
        task.setCollectorNotes(notes);
        taskRepository.save(task);

        LabOrder order = orderRepository.findById(task.getLabOrderId()).orElseThrow();
        order.setStatus(LabOrderStatus.COLLECTED);
        orderRepository.save(order);

        // Generate Samples
        // Logic: Create one sample per item for simplicity, or 1 sample per type.
        // I will assume 1 sample per item for this demo to carry test code.
        // Or better: Group by SampleType.
        // For simplicity: One Sample per Order (representing the box/tube set).
        // Let's create one Sample Entity for the Order.

        Sample sample = new Sample();
        sample.setLabOrderId(order.getId());
        sample.setBarcode("SAMP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        sample.setSampleType(SampleType.BLOOD); // Defaulting for MVP or deriving
        sample.setStatus(SampleStatus.COLLECTED);
        sample.setCollectedAt(LocalDateTime.now());
        sampleRepository.save(sample);

        auditService.logAction(AuditAction.COLLECT, "CollectionTask", taskId, "Sample Collected");
        kafkaProducer.publish("sample-collected", sample.getBarcode());
    }

    @Transactional
    public void markFailed(String taskId, String reason) {
        CollectionTask task = taskRepository.findById(UUID.fromString(taskId))
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        task.setStatus(CollectionTaskStatus.FAILED);
        task.setCollectorNotes(reason);
        taskRepository.save(task);

        // Order remains? or Cancelled? Usually reschedule.
        // audit...
    }
}
