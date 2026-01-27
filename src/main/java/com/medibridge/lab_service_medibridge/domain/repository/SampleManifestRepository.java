package com.medibridge.lab_service_medibridge.domain.repository;

import com.medibridge.lab_service_medibridge.domain.SampleManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SampleManifestRepository extends JpaRepository<SampleManifest, UUID> {
    List<SampleManifest> findByTaskId(UUID taskId);
}
