package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.SampleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lab_test_catalog", indexes = {
        @Index(name = "idx_test_code", columnList = "testCode", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String testCode;

    @Column(nullable = false)
    private String testName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SampleType sampleType;

    @Column(nullable = false)
    private boolean fastingRequired;

    private Integer expectedReportTimeHours;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    private boolean active = true;
}
