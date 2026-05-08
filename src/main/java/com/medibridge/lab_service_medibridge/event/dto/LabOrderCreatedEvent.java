package com.medibridge.lab_service_medibridge.event.dto;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class LabOrderCreatedEvent {
    private String orderId;
    private String orderNumber;
    private String patientId;
    private String doctorId;
    private CollectionType collectionType;
    private LocalDateTime createdAt;
    private String status;
}
