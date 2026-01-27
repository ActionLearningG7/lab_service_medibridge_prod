package com.medibridge.lab_service_medibridge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing lab test results with file uploads
 * Supports multiple versions/uploads per lab order
 */
@Entity
@Table(name = "lab_results", indexes = {
        @Index(name = "idx_lab_result_order_id", columnList = "lab_order_id"),
        @Index(name = "idx_lab_result_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id", updatable = false, nullable = false)
    private UUID resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    // File storage info (Cloudinary)
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType; // e.g., application/pdf, image/jpeg

    @Column(name = "file_size")
    private Long fileSize; // in bytes

    // Metadata
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_latest", nullable = false)
    @Builder.Default
    private Boolean isLatest = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Upload info
    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy; // Phlebotomist admin user ID

    @Column(name = "uploaded_by_name", length = 200)
    private String uploadedByName;

    // Audit
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.isLatest = false;
    }
}
