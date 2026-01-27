package com.medibridge.lab_service_medibridge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "location_pings", indexes = {
        @Index(name = "idx_task_captured", columnList = "task_id, captured_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LocationPing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "phlebotomist_id", nullable = false)
    private String phlebotomistId;

    private Double latitude;
    private Double longitude;

    private Double accuracy; // meters
    private Double speed; // m/s
    private Double bearing; // degrees

    @CreatedDate
    @Column(name = "captured_at", nullable = false, updatable = false)
    private LocalDateTime capturedAt;
}
