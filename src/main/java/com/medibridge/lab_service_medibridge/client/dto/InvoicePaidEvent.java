package com.medibridge.lab_service_medibridge.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaidEvent {
    private Long invoiceId;
    private String patientId;
    private String invoiceNumber;
    private BigDecimal amount;
    private String currency;
    private String stripePaymentIntentId;
    private Long paymentTransactionId;
    private LocalDateTime paidAt;
    private String source;
    private String serviceType;
    private String serviceRefId;
}
