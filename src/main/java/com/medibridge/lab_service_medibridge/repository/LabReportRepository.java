package com.medibridge.lab_service_medibridge.repository;

import com.medibridge.lab_service_medibridge.domain.LabReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabReportRepository extends JpaRepository<LabReport, UUID> {

    Optional<LabReport> findByLabOrderId(UUID labOrderId);
}
