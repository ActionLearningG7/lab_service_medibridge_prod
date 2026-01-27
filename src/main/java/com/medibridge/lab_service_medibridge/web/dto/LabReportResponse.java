package com.medibridge.lab_service_medibridge.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for lab report response
 * Returns metadata without exposing internal IDs or sensitive info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReportResponse {

    private UUID reportId;
    private UUID labOrderId;
    private String status;

    /**
     * Secure URL to access report (only for authorized users)
     * Only returned if report is PUBLISHED and user is authorized
     */
    private String reportUrl;

    private String mimeType;
    private Long sizeBytes;
    private String format;
    private String originalFilename;

    /**
     * Checksum for integrity verification
     */
    private String checksum;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * For images, include dimensions
     */
    private Integer width;
    private Integer height;
}
