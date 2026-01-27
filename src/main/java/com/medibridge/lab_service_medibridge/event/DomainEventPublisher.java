package com.medibridge.lab_service_medibridge.event;

import com.medibridge.lab_service_medibridge.event.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final KafkaProducer kafkaProducer;

    public void publishTaskCreated(UUID taskId, UUID orderId) {
        CollectionTaskCreatedEvent event = CollectionTaskCreatedEvent.builder()
                .taskId(taskId)
                .orderId(orderId)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publish("collection.task.created", event);
    }

    public void publishTaskAssigned(UUID taskId, String phlebotomistId) {
        CollectionTaskAssignedEvent event = CollectionTaskAssignedEvent.builder()
                .taskId(taskId)
                .phlebotomistId(phlebotomistId)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publish("collection.task.assigned", event);
    }

    public void publishStatusChanged(UUID taskId,
            com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus oldStatus,
            com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus newStatus) {
        CollectionTaskStatusChangedEvent event = CollectionTaskStatusChangedEvent.builder()
                .taskId(taskId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publish("collection.task.status.changed", event);
    }

    public void publishReportUploaded(UUID orderId, UUID reportId) {
        ReportUploadedEvent event = ReportUploadedEvent.builder()
                .orderId(orderId)
                .reportId(reportId)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publish("report.uploaded", event);
    }

    public void publishReportPublished(UUID orderId, UUID reportId) {
        ReportPublishedEvent event = ReportPublishedEvent.builder()
                .orderId(orderId)
                .reportId(reportId)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publish("report.published", event);
    }
}
