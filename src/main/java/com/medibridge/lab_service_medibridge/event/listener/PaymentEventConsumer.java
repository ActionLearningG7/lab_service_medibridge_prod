package com.medibridge.lab_service_medibridge.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibridge.lab_service_medibridge.client.dto.InvoicePaidEvent;
import com.medibridge.lab_service_medibridge.service.LabOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final LabOrderService labOrderService;

    @KafkaListener(topics = "invoice.paid", groupId = "lab-payment-group")
    public void consumePaymentEvent(String message) {
        try {
            log.info("Received Payment Paid Event: {}", message);
            InvoicePaidEvent event = objectMapper.readValue(message, InvoicePaidEvent.class);

            if ("LAB_ORDER".equals(event.getServiceType()) && event.getServiceRefId() != null) {
                log.info("Processing successful payment for lab order: {}", event.getServiceRefId());
                labOrderService.processPaymentSuccess(UUID.fromString(event.getServiceRefId()));
            }
        } catch (Exception e) {
            log.error("Failed to process payment event in Lab Service: {}", message, e);
        }
    }
}
