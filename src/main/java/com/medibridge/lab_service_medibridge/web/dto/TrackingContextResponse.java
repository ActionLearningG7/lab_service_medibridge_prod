package com.medibridge.lab_service_medibridge.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracking context for patient/doctor viewing order tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingContextResponse {

    // Hospital (origin/return point)
    private LocationDto hospitalLocation;

    // Patient destination
    private LocationDto destinationLocation;

    // Current order status
    private String status;

    // Phlebotomist summary
    private PhlebotomistSummary phlebotomist;

    // Estimated arrival (from last calculation)
    private Integer estimatedArrivalMinutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private Double lat;
        private Double lng;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhlebotomistSummary {
        private String id;
        private String name;
        private String phone;
    }
}
