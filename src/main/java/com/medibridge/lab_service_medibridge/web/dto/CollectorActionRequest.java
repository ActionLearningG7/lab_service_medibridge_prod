package com.medibridge.lab_service_medibridge.web.dto;

import lombok.Data;

@Data
public class CollectorActionRequest {
    private String notes;
    private String reason; // For failure
    // private String otp; // future
}
