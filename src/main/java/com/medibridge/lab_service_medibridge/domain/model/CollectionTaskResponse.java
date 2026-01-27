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
public class CollectionTaskResponse {
    private UUID taskId;
    private UUID labOrderId;
    private CollectionTaskStatus status;
    private String phlebotomistId;

    private LocalDateTime requestedSlot;
    private LocalDateTime scheduledAt;
    private Integer priority;

    // Status Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime collectedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;
    private String cancelReason;

    private String notesForPhlebotomist;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TestDetail> tests;

    // Enriched Patient Data
    private String patientName;
    private String patientPhone;
    private String patientAge;

    // Address Data
    private AddressDetail address;

    // Instructions
    private InstructionDetail instructions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressDetail {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String zipCode;
        private Coordinates coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Coordinates {
        private Double lat;
        private Double lng;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstructionDetail {
        private String fastingInstructions;
        private String generalNotes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestDetail {
        private UUID id;
        private String name;
        private String code;
    }
}
