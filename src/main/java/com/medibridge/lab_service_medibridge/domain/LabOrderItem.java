package com.medibridge.lab_service_medibridge.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lab_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @Column(nullable = false)
    private String testCode;

    // Snapshot fields
    @Column(nullable = false)
    private String testName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private boolean fastingRequired;
}
