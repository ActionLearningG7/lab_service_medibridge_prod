package com.medibridge.lab_service_medibridge.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleCollectionRequest {

    @NotEmpty(message = "At least one sample must be collected")
    @Valid
    private List<SampleItem> samples;

    private boolean otpVerified;

    // Optional temperature reading
    @DecimalMin("-100.0")
    @DecimalMax("100.0")
    private Double temperatureCelsius;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SampleItem {
        @NotBlank(message = "Barcode is required")
        private String barcode;

        @NotBlank(message = "Sample type is required")
        private String sampleType; // e.g., BLOOD, URINE

        @NotBlank(message = "Container type is required")
        private String containerType; // e.g., EDTA_TUBE, CUP

        private int quantity = 1;
    }
}
