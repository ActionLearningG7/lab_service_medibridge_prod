package com.medibridge.lab_service_medibridge.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for lab results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {

    private UUID resultId;
    private UUID labOrderId;
    private String orderNumber;

    // File info
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // Metadata
    private Integer version;
    private Boolean isLatest;
    private String notes;

    // Upload info
    private String uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
