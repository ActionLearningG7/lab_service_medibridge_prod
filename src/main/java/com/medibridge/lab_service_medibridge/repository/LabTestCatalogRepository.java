package com.medibridge.lab_service_medibridge.repository;

import com.medibridge.lab_service_medibridge.domain.LabTestCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabTestCatalogRepository extends JpaRepository<LabTestCatalog, UUID> {
    Optional<LabTestCatalog> findByTestCode(String testCode);
}
