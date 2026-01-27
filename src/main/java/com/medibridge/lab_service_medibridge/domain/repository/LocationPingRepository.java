package com.medibridge.lab_service_medibridge.domain.repository;

import com.medibridge.lab_service_medibridge.domain.LocationPing;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationPingRepository extends JpaRepository<LocationPing, UUID> {

    // Find history for a specific task, ordered by time
    List<LocationPing> findByTaskIdOrderByCapturedAtDesc(UUID taskId, Pageable pageable);
}
