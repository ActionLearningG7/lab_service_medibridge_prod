package com.medibridge.lab_service_medibridge.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskAssignmentRequest {
    @NotBlank(message = "Phlebotomist ID is required")
    private String phlebotomistId;
}
