package com.medibridge.lab_service_medibridge.repository;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, UUID>, JpaSpecificationExecutor<LabOrder> {

    Optional<LabOrder> findByOrderNumber(String orderNumber);

    Page<LabOrder> findByPatientId(String patientId, Pageable pageable);

    Page<LabOrder> findByDoctorId(String doctorId, Pageable pageable);

    java.util.List<LabOrder> findByPatientId(String patientId);
}
