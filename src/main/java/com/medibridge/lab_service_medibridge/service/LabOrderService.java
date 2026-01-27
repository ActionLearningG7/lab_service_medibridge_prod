package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.*;
import com.medibridge.lab_service_medibridge.domain.enums.*;
import com.medibridge.lab_service_medibridge.event.KafkaProducer;
import com.medibridge.lab_service_medibridge.event.dto.LabOrderCreatedEvent;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.repository.LabTestCatalogRepository;
// import com.medibridge.lab_service_medibridge.util.SecurityUtils; // Removed
import com.medibridge.lab_service_medibridge.web.dto.LabOrderRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabTestCatalogRepository catalogRepository;
    private final CollectionTaskRepository taskRepository;
    private final AuditService auditService;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public LabOrder createOrder(LabOrderRequest request, String actorId, boolean isDoctor) {

        LabOrder order = new LabOrder();
        order.setOrderNumber("LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        if (isDoctor) {
            order.setDoctorId(actorId);
            order.setPatientId(request.getPatientId());
            order.setAppointmentId(request.getAppointmentId());
        } else {
            order.setPatientId(actorId);
            // Patient booking for self
        }

        order.setCollectionType(request.getCollectionType());
        order.setStatus(LabOrderStatus.CREATED);

        // Address Snapshot
        order.setAddressLine1(request.getAddressLine1());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPostalCode(request.getPostalCode());
        order.setCountry(request.getCountry());
        order.setContactPhone(request.getContactPhone());
        order.setSpecialInstructions(request.getSpecialInstructions());

        if (request.getLatitude() != null) {
            order.setDeliveryLat(java.math.BigDecimal.valueOf(request.getLatitude()));
        }
        if (request.getLongitude() != null) {
            order.setDeliveryLng(java.math.BigDecimal.valueOf(request.getLongitude()));
        }

        order.setCreatedBy(actorId);

        if (request.getCollectionType() == CollectionType.HOME) {
            order.setPreferredSlotStart(request.getPreferredSlotStart());
            order.setPreferredSlotEnd(request.getPreferredSlotEnd());
            order.setStatus(LabOrderStatus.SCHEDULED);
        }

        // Add Items
        if (request.getTestCodes() != null) {
            for (String testCode : request.getTestCodes()) {
                LabTestCatalog catalog = catalogRepository.findByTestCode(testCode)
                        .orElseThrow(() -> new EntityNotFoundException("Test code not found: " + testCode));

                LabOrderItem item = new LabOrderItem();
                item.setTestCode(catalog.getTestCode());
                item.setTestName(catalog.getTestName());
                item.setPrice(catalog.getPrice());
                item.setFastingRequired(catalog.isFastingRequired());

                order.addItem(item);
            }
        }

        LabOrder saved = labOrderRepository.save(order);

        // Create Collection Task if HOME
        if (saved.getCollectionType() == CollectionType.HOME) {
            CollectionTask task = new CollectionTask();
            task.setLabOrderId(saved.getId());
            task.setStatus(CollectionTaskStatus.CREATED);
            task.setScheduledAt(request.getPreferredSlotStart());
            taskRepository.save(task);
        }

        auditService.logAction(AuditAction.CREATE, "LabOrder", saved.getId().toString(), "Order Created by " + actorId);

        // Emit Event
        LabOrderCreatedEvent event = LabOrderCreatedEvent.builder()
                .orderId(saved.getId().toString())
                .orderNumber(saved.getOrderNumber())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .collectionType(saved.getCollectionType())
                .createdAt(LocalDateTime.now())
                .build();

        kafkaProducer.publish("lab-order-created", event);

        return saved;
    }

    @Transactional
    public void cancelOrder(String id) {
        LabOrder order = labOrderRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (order.getStatus().ordinal() >= LabOrderStatus.COLLECTED.ordinal()) {
            throw new IllegalStateException("Cannot cancel order after collection");
        }

        order.setStatus(LabOrderStatus.CANCELLED);
        labOrderRepository.save(order);

        taskRepository.findByLabOrderId(order.getId()).ifPresent(task -> {
            task.setStatus(CollectionTaskStatus.CANCELLED);
            taskRepository.save(task);
        });

        auditService.logAction(AuditAction.CANCEL, "LabOrder", id, "Order Cancelled");

        kafkaProducer.publish("lab-order-cancelled", id);
    }

    @Transactional(readOnly = true)
    public LabOrder getOrder(String id) {
        return labOrderRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}
