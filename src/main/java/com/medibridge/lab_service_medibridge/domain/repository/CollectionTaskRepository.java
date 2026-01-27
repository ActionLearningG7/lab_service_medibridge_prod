package com.medibridge.lab_service_medibridge.domain.repository;

import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionTaskRepository extends JpaRepository<CollectionTask, UUID> {

    Optional<CollectionTask> findByLabOrderId(UUID labOrderId);

    List<CollectionTask> findByPhlebotomistIdAndStatus(String phlebotomistId, CollectionTaskStatus status);

    List<CollectionTask> findByStatus(CollectionTaskStatus status);

    org.springframework.data.domain.Page<CollectionTask> findByStatus(CollectionTaskStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t FROM CollectionTask t WHERE t.phlebotomistId = :phlebId AND t.status IN :statuses")
    List<CollectionTask> findActiveTasksForPhlebotomist(String phlebId, List<CollectionTaskStatus> statuses);

    @Query("SELECT t FROM CollectionTask t WHERE t.status = 'CREATED' AND t.scheduledAt BETWEEN :start AND :end")
    List<CollectionTask> findUnassignedTasksInWindow(LocalDateTime start, LocalDateTime end);
}
