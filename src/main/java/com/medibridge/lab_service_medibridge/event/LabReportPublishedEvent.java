package com.medibridge.lab_service_medibridge.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event published when lab report is published
 * Notifies patient/doctor that report is ready
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReportPublishedEvent {
    private String reportId;
    private String labOrderId;
    private String patientId;
    private String doctorId;
    private LocalDateTime publishedAt;
}
