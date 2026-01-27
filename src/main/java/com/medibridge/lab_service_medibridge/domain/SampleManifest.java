package com.medibridge.lab_service_medibridge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sample_manifests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SampleManifest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    // Storing as JSON or simplified text list for simplicity in this iteration
    // Format: "EDTA-TUBE:2:BAR123,URINE-CUP:1:BAR456"
    @Column(columnDefinition = "TEXT")
    private String sampleData;

    private boolean pickupVerifiedByOtp;

    private LocalDateTime verifiedAt;

    @Column(length = 36)
    private String collectedBy; // Phleb ID

    private LocalDateTime collectedAt;

    // Optional chain of custody fields
    private Double temperatureAtCollection;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
