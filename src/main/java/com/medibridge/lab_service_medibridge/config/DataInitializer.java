package com.medibridge.lab_service_medibridge.config;

import com.medibridge.lab_service_medibridge.domain.LabTestCatalog;
import com.medibridge.lab_service_medibridge.domain.enums.SampleType;
import com.medibridge.lab_service_medibridge.repository.LabTestCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final LabTestCatalogRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            log.info("Initializing Lab Test Catalog...");

            repository.save(LabTestCatalog.builder()
                    .testCode("TEST-CBC")
                    .testName("Complete Blood Count")
                    .description("Evaluates overall health and detects a wide range of disorders.")
                    .sampleType(SampleType.BLOOD)
                    .fastingRequired(false)
                    .expectedReportTimeHours(24)
                    .price(new BigDecimal("15.00"))
                    .active(true)
                    .build());

            repository.save(LabTestCatalog.builder()
                    .testCode("TEST-LIPID")
                    .testName("Lipid Panel")
                    .description("Measures fats and fatty substances used as a source of energy.")
                    .sampleType(SampleType.BLOOD)
                    .fastingRequired(true)
                    .expectedReportTimeHours(24)
                    .price(new BigDecimal("25.00"))
                    .active(true)
                    .build());

            repository.save(LabTestCatalog.builder()
                    .testCode("TEST-TSH")
                    .testName("Thyroid Stimulating Hormone")
                    .description("Screening test for thyroid function.")
                    .sampleType(SampleType.BLOOD)
                    .fastingRequired(false)
                    .expectedReportTimeHours(48)
                    .price(new BigDecimal("20.00"))
                    .active(true)
                    .build());

            log.info("Lab Test Catalog initialized.");
        }
    }
}
