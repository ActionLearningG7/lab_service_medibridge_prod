package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.SampleStatus;
import com.medibridge.lab_service_medibridge.domain.enums.SampleType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "samples", indexes = {
        @Index(name = "idx_barcode", columnList = "barcode", unique = true),
        @Index(name = "idx_sample_order", columnList = "labOrderId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID labOrderId;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SampleType sampleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SampleStatus status;

    private LocalDateTime collectedAt;
    private LocalDateTime receivedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
