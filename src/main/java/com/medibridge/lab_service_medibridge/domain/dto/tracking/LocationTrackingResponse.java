package com.medibridge.lab_service_medibridge.domain.dto.tracking;

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
public class LocationTrackingResponse {
    private UUID orderId;
    private UUID taskId;
    private String taskStatus;

    // Locations
    private GeoPoint patientLocation;
    private GeoPoint phlebotomistLocation;

    // Route for map (list of points)
    private List<GeoPoint> route;

    // Metadata
    private String eta;
    private LocalDateTime lastUpdated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private Double lat;
        private Double lng;
    }
}
