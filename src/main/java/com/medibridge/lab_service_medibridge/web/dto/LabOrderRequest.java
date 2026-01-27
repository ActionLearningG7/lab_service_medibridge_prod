package com.medibridge.lab_service_medibridge.web.dto;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionType;
// import jakarta.validation.constraints.NotBlank; // Removed
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LabOrderRequest {

    // For Doctor usage
    private String patientId;
    private String appointmentId;

    @NotEmpty(message = "At least one test must be selected")
    private List<String> testCodes;

    @NotNull(message = "Collection type is required")
    private CollectionType collectionType;

    private LocalDateTime preferredSlotStart;
    private LocalDateTime preferredSlotEnd;

    // Address info (required for HOME collection)
    private String addressLine1;
    private String postalCode;
    private String city;
    private String state;
    private String country;

    private String contactPhone;

    private String specialInstructions;

    // Coordinates
    private Double latitude;
    private Double longitude;
}
