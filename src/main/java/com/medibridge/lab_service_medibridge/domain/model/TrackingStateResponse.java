package com.medibridge.lab_service_medibridge.domain.model;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingStateResponse {

    private UUID labOrderId;
    private UUID taskId;
    private CollectionTaskStatus status;

    private LocationMinimal lastLocation; // Phlebotomist live location

    // Ordered list of status changes (history) could be added here if we had an
    // audit table query
    // For now, we return the timestamp of the *current* relevant phase
    private LocalDateTime lastUpdatedAt;

    // ETA in minutes, if available (e.g. from Google Maps API or heuristic)
    private Integer estimatedArrivalMinutes;

    // Patient's resolved location (destination)
    private LocationMinimal patientLocation;

    // Hospital location (origin/return point)
    private LocationMinimal hospitalLocation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationMinimal {
        private Double latitude;
        private Double longitude;
        private LocalDateTime capturedAt;
    }
}
