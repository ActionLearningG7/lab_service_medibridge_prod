package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.*;
import com.medibridge.lab_service_medibridge.domain.enums.*;
import com.medibridge.lab_service_medibridge.event.KafkaProducer;
import com.medibridge.lab_service_medibridge.event.dto.LabOrderCreatedEvent;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.repository.LabTestCatalogRepository;
// import com.medibridge.lab_service_medibridge.util.SecurityUtils; // Removed
import com.medibridge.lab_service_medibridge.client.dto.CheckoutRequest;
import com.medibridge.lab_service_medibridge.client.dto.CheckoutResponse;
import com.medibridge.lab_service_medibridge.client.dto.CreateInvoiceRequest;
import com.medibridge.lab_service_medibridge.client.dto.InvoiceDTO;
import com.medibridge.lab_service_medibridge.client.feign.PaymentServiceClient;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderRequest;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import com.medibridge.lab_service_medibridge.web.mapper.LabOrderMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
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
    private final PaymentServiceClient paymentServiceClient;
    private final LabOrderMapper mapper;

    private final Logger log = org.slf4j.LoggerFactory.getLogger(LabOrderService.class);

    @Transactional
    public LabOrderResponse createOrder(LabOrderRequest request, String actorId, boolean isDoctor) {

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
        }

        order.setStatus(LabOrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus("PENDING");

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

        String checkoutUrl = null;
        String invoiceId = null;

        // Create Invoice in Payment Service
        try {
            List<CreateInvoiceRequest.CreateInvoiceItemRequest> invoiceItems = new ArrayList<>();

            // Add Tests
            for (LabOrderItem item : saved.getItems()) {
                invoiceItems.add(CreateInvoiceRequest.CreateInvoiceItemRequest.builder()
                        .description(item.getTestName())
                        .quantity(1)
                        .unitPrice(item.getPrice())
                        .category("LAB_TEST")
                        .build());
            }

            // Add Collection Fee if HOME
            if (saved.getCollectionType() == CollectionType.HOME) {
                invoiceItems.add(CreateInvoiceRequest.CreateInvoiceItemRequest.builder()
                        .description("Home Collection Fee")
                        .quantity(1)
                        .unitPrice(new BigDecimal("15.00"))
                        .category("COLLECTION_FEE")
                        .build());
            }

            CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.builder()
                    .patientId(saved.getPatientId())
                    .description("Lab Order - " + saved.getOrderNumber())
                    .dueDate(LocalDateTime.now().plusHours(48))
                    .serviceType("LAB_ORDER")
                    .serviceRefId(saved.getId().toString())
                    .items(invoiceItems)
                    .build();

            InvoiceDTO invoice = paymentServiceClient.createInvoice(invoiceRequest);
            invoiceId = invoice.getId().toString();
            saved.setInvoiceId(invoiceId);
            labOrderRepository.save(saved);
            log.info("Linked invoice {} to lab order {}", invoice.getId(), saved.getId());

            // Get Checkout Link
            CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                    .invoiceId(invoice.getId())
                    .successUrl("http://localhost:3000/payments/success?ref=LAB_ORDER&id=" + saved.getId())
                    .cancelUrl("http://localhost:3000/payments/cancel?ref=LAB_ORDER&id=" + saved.getId())
                    .build();

            CheckoutResponse checkoutResponse = paymentServiceClient.getCheckoutLink(checkoutRequest);
            checkoutUrl = checkoutResponse.getCheckoutUrl();

        } catch (Exception e) {
            log.error("Failed to create invoice or checkout link for lab order: {}", saved.getId(), e);
        }

        auditService.logAction(AuditAction.CREATE, "LabOrder", saved.getId().toString(),
                "Order Created (Payment Pending) by " + actorId);

        // Emit Event
        LabOrderCreatedEvent event = LabOrderCreatedEvent.builder()
                .orderId(saved.getId().toString())
                .orderNumber(saved.getOrderNumber())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .collectionType(saved.getCollectionType())
                .createdAt(LocalDateTime.now())
                .status("PAYMENT_PENDING")
                .build();

        kafkaProducer.publish("lab-order-created", event);

        // Prepare response
        LabOrderResponse response = mapper.toResponse(saved);
        response.setPaymentRequired(true);
        response.setInvoiceId(invoiceId);
        response.setCheckoutUrl(checkoutUrl);

        return response;
    }

    @Transactional
    public void processPaymentSuccess(UUID orderId) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (order.getStatus() == LabOrderStatus.PAYMENT_PENDING) {
            order.setStatus(LabOrderStatus.CONFIRMED);
            order.setPaymentStatus("PAID");

            if (order.getCollectionType() == CollectionType.HOME) {
                order.setStatus(LabOrderStatus.SCHEDULED);

                // Trigger creation of CollectionTask only after payment
                CollectionTask task = new CollectionTask();
                task.setLabOrderId(order.getId());
                task.setStatus(CollectionTaskStatus.CREATED);
                task.setScheduledAt(order.getPreferredSlotStart());
                taskRepository.save(task);
                log.info("CollectionTask created for paid lab order: {}", orderId);
            }

            labOrderRepository.save(order);
            log.info("Lab order {} marked as PAID and CONFIRMED", orderId);

            auditService.logAction(AuditAction.UPDATE, "LabOrder", orderId.toString(),
                    "Payment Confirmed - Order Confirmed");

            // Optionally notify other services here if needed
        }
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
