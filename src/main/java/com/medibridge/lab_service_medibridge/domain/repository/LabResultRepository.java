package com.medibridge.lab_service_medibridge.domain.repository;

import com.medibridge.lab_service_medibridge.domain.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {

    /**
     * Find all results for a lab order
     */
    @Query("SELECT lr FROM LabResult lr WHERE lr.labOrder.id = :orderId AND lr.deleted = false ORDER BY lr.version DESC")
    List<LabResult> findByLabOrderId(@Param("orderId") UUID orderId);

    /**
     * Find the latest result for a lab order
     */
    @Query("SELECT lr FROM LabResult lr WHERE lr.labOrder.id = :orderId AND lr.isLatest = true AND lr.deleted = false")
    Optional<LabResult> findLatestByLabOrderId(@Param("orderId") UUID orderId);

    /**
     * Find next version number for a lab order
     */
    @Query("SELECT COALESCE(MAX(lr.version), 0) + 1 FROM LabResult lr WHERE lr.labOrder.id = :orderId")
    Integer findNextVersionForOrder(@Param("orderId") UUID orderId);

    /**
     * Count results for a lab order
     */
    @Query("SELECT COUNT(lr) FROM LabResult lr WHERE lr.labOrder.id = :orderId AND lr.deleted = false")
    Long countByLabOrderId(@Param("orderId") UUID orderId);
}
