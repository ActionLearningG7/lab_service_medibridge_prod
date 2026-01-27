package com.medibridge.lab_service_medibridge.domain.model;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhlebotomistTaskSummaryResponse {
    private UUID taskId;
    private UUID labOrderId;
    private CollectionTaskStatus status;
    private Integer priority;

    private LocalDateTime scheduledAt;

    // Minimal address info needed for list view
    private String address;
    private String area; // e.g., "Downtown"
    private String city;

    // For map view
    private Double patientLatitude;
    private Double patientLongitude;
}
