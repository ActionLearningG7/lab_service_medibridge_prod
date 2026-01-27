package com.medibridge.lab_service_medibridge.util;

import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.model.CollectionTaskResponse;
import com.medibridge.lab_service_medibridge.domain.model.PhlebotomistTaskSummaryResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for Lab Service Domain objects
 * (Keeping it simple without MapStruct for now, easy to debug)
 */
@Component
public class LabMapper {

    public CollectionTaskResponse toTaskResponse(CollectionTask task) {
        if (task == null)
            return null;

        return CollectionTaskResponse.builder()
                .taskId(task.getId())
                .labOrderId(task.getLabOrderId())
                .status(task.getStatus())
                .phlebotomistId(task.getPhlebotomistId())
                .requestedSlot(task.getRequestedSlot())
                .scheduledAt(task.getScheduledAt())
                .priority(task.getPriority())
                .assignedAt(task.getAssignedAt())
                .acceptedAt(task.getAcceptedAt())
                .enRouteAt(task.getEnRouteAt())
                .arrivedAt(task.getArrivedAt())
                .collectedAt(task.getCollectedAt())
                .deliveredAt(task.getDeliveredAt())
                .completedAt(task.getCompletedAt())
                .cancelledAt(task.getCancelledAt())
                .cancelReason(task.getCancelReason())
                .notesForPhlebotomist(task.getNotesForPhlebotomist())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // Requires LabOrder to fill address details
    public PhlebotomistTaskSummaryResponse toPhlebSummary(CollectionTask task, String address, String city, String area,
            Double lat,
            Double lng) {
        if (task == null)
            return null;

        return PhlebotomistTaskSummaryResponse.builder()
                .taskId(task.getId())
                .labOrderId(task.getLabOrderId())
                .status(task.getStatus())
                .priority(task.getPriority())
                .scheduledAt(task.getScheduledAt())
                .address(address)
                .city(city)
                .area(area)
                .patientLatitude(lat)
                .patientLongitude(lng)
                .build();
    }
}
