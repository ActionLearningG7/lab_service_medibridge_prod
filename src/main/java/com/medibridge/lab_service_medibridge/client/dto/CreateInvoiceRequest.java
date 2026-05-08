package com.medibridge.lab_service_medibridge.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {
    private String patientId;
    private String description;
    private LocalDateTime dueDate;
    private List<CreateInvoiceItemRequest> items;
    private String serviceType;
    private String serviceRefId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInvoiceItemRequest {
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String category;
    }
}
