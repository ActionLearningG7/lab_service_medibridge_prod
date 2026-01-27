package com.medibridge.lab_service_medibridge.domain.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTaskRequest {

    @NotBlank(message = "Phlebotomist ID is required")
    private String phlebotomistId;

    @FutureOrPresent(message = "Schedule time must be in the future or present")
    private LocalDateTime scheduledAt;

    private String notesForPhlebotomist;

    // 0=NORMAL, 1=URGENT. If null, keep existing.
    private Integer priority;
}
