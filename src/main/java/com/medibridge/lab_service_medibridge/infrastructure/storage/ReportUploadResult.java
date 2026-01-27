package com.medibridge.lab_service_medibridge.infrastructure.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of successful report upload to cloud storage
 * Contains all metadata needed for LabReport entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportUploadResult {

    /**
     * Secure URL to access the report (HTTPS)
     * This URL should only be shared with authorized users
     */
    private String secureUrl;

    /**
     * Cloudinary public ID for resource management
     * Used for deletion and updates
     */
    private String publicId;

    /**
     * Resource type (raw, image, video, etc.)
     */
    private String resourceType;

    /**
     * File size in bytes
     */
    private Long bytes;

    /**
     * File format (pdf, png, jpg, etc.)
     */
    private String format;

    /**
     * Original filename as uploaded
     */
    private String originalFilename;

    /**
     * MIME type (application/pdf, image/png, etc.)
     */
    private String mimeType;

    /**
     * SHA-256 checksum for integrity verification
     */
    private String checksum;

    /**
     * Width (for images only)
     */
    private Integer width;

    /**
     * Height (for images only)
     */
    private Integer height;

    /**
     * Timestamp when uploaded
     */
    private Long createdAt;
}
