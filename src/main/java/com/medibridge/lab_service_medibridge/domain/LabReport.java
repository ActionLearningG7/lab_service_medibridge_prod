package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.LabReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_reports", indexes = {
        @Index(name = "idx_report_order", columnList = "labOrderId"),
        @Index(name = "idx_report_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LabReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID labOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabReportStatus status;

    // Cloudinary secure URL - only accessible to authorized users
    @Column(length = 500)
    private String reportUrl;

    // Cloudinary public ID for resource management
    @Column(length = 255)
    private String cloudinaryPublicId;

    // MIME type (application/pdf, image/png, etc.)
    @Column(length = 100)
    private String reportMimeType;

    // File size in bytes
    private Long reportSizeBytes;

    // SHA-256 checksum for integrity verification
    @Column(length = 64)
    private String checksum;

    // Original filename uploaded
    @Column(length = 255)
    private String originalFilename;

    // Cloudinary resource type (raw, image, etc.)
    @Column(length = 20)
    private String resourceType;

    // Format (pdf, png, jpg, etc.)
    @Column(length = 20)
    private String format;

    @Column(columnDefinition = "JSON")
    private String reportData; // Structured JSON for test results

    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper method to check if report is accessible
    public boolean isPublished() {
        return status == LabReportStatus.PUBLISHED;
    }
}
