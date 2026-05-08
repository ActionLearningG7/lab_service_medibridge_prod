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
public class InvoiceDTO {
    private Long id;
    private String patientId;
    private String invoiceNumber;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String currency;
    private String description;
    private LocalDateTime dueDate;
    private String serviceType;
    private String serviceRefId;
}
