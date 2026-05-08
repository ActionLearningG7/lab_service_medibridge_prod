package com.medibridge.lab_service_medibridge.web.dto;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionType;
import com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
public class LabOrderResponse {
    private String id;
    private String orderNumber;
    private String patientId;
    private String doctorId;
    private LabOrderStatus status;
    private CollectionType collectionType;
    private LocalDateTime preferredSlotStart;
    private LocalDateTime preferredSlotEnd;
    private String addressLine1;
    private String city;
    private String state;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal totalPrice;
    private List<LabOrderItemDto> items;
    private LocalDateTime createdAt;

    private List<LabResultResponse> results;
    private boolean paymentRequired;
    private String invoiceId;
    private String checkoutUrl;
}
