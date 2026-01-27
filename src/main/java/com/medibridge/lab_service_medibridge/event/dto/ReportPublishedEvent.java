package com.medibridge.lab_service_medibridge.event.dto;

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
public class ReportPublishedEvent {
    private UUID orderId;
    private UUID reportId;
    private LocalDateTime timestamp;
}
