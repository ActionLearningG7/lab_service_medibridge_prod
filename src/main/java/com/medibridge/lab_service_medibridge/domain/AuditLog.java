package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_target_id", columnList = "targetId"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 36)
    private String actorId;

    @Column(nullable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction actionType;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false, length = 36)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
