package com.medibridge.lab_service_medibridge.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Navigation context for phlebotomist navigation view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationContextResponse {

    // Destination (patient location)
    private LocationDto destination;

    // Hospital (origin/return point)
    private LocationDto hospital;

    // Order details
    private String orderNumber;
    private String patientName;
    private String patientPhone;
    private String specialInstructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private Double lat;
        private Double lng;
        private String address;
    }
}
