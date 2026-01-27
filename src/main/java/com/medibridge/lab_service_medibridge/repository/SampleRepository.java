package com.medibridge.lab_service_medibridge.repository;

import com.medibridge.lab_service_medibridge.domain.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SampleRepository extends JpaRepository<Sample, UUID> {

    Optional<Sample> findByBarcode(String barcode);

    Optional<Sample> findByLabOrderId(UUID labOrderId);
}
