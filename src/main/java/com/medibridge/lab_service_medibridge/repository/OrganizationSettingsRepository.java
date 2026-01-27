package com.medibridge.lab_service_medibridge.repository;

import com.medibridge.lab_service_medibridge.domain.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {
    Optional<OrganizationSettings> findFirstByOrderByIdAsc();
}
