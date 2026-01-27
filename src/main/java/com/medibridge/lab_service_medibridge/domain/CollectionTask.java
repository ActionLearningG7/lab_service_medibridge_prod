package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_tasks", indexes = {
        @Index(name = "idx_task_lab_order", columnList = "lab_order_id", unique = true),
        @Index(name = "idx_phlebotomist", columnList = "phlebotomist_id"),
        @Index(name = "idx_task_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CollectionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lab_order_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID labOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionTaskStatus status;

    @Column(name = "phlebotomist_id", length = 36)
    private String phlebotomistId; // Assigned collector user ID

    // Schedule
    private LocalDateTime requestedSlot;
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0; // 0=NORMAL, 1=URGENT

    // Tracking Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime collectedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;

    // Cancellation
    private LocalDateTime cancelledAt;
    private String cancelledByRole;
    private String cancelReason;

    // Metadata
    @Column(columnDefinition = "TEXT")
    private String notesForPhlebotomist;

    @Column(columnDefinition = "TEXT")
    private String collectionInstructions;

    @Column(columnDefinition = "TEXT")
    private String collectorNotes;

    private String patientConfirmationOtpHash;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
