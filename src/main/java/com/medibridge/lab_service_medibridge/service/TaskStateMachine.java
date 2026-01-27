package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces strict transitions for CollectionTask logistics
 */
@Component
public class TaskStateMachine {

    private static final Map<CollectionTaskStatus, Set<CollectionTaskStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(CollectionTaskStatus.CREATED,
                    EnumSet.of(CollectionTaskStatus.ASSIGNED, CollectionTaskStatus.CANCELLED)),
            Map.entry(CollectionTaskStatus.ASSIGNED,
                    EnumSet.of(CollectionTaskStatus.ACCEPTED, CollectionTaskStatus.CANCELLED)),
            Map.entry(CollectionTaskStatus.ACCEPTED,
                    EnumSet.of(CollectionTaskStatus.EN_ROUTE, CollectionTaskStatus.CANCELLED)),
            Map.entry(CollectionTaskStatus.EN_ROUTE,
                    EnumSet.of(CollectionTaskStatus.ARRIVED, CollectionTaskStatus.CANCELLED)),
            Map.entry(CollectionTaskStatus.ARRIVED,
                    EnumSet.of(CollectionTaskStatus.SAMPLES_COLLECTED, CollectionTaskStatus.CANCELLED,
                            CollectionTaskStatus.FAILED)),
            Map.entry(CollectionTaskStatus.SAMPLES_COLLECTED,
                    EnumSet.of(CollectionTaskStatus.IN_TRANSIT, CollectionTaskStatus.DELIVERED_TO_LAB)),
            Map.entry(CollectionTaskStatus.IN_TRANSIT,
                    EnumSet.of(CollectionTaskStatus.DELIVERED_TO_LAB, CollectionTaskStatus.FAILED)),
            Map.entry(CollectionTaskStatus.DELIVERED_TO_LAB, EnumSet.of(CollectionTaskStatus.COMPLETED)),
            Map.entry(CollectionTaskStatus.COMPLETED, EnumSet.noneOf(CollectionTaskStatus.class)),
            Map.entry(CollectionTaskStatus.CANCELLED, EnumSet.noneOf(CollectionTaskStatus.class)),
            Map.entry(CollectionTaskStatus.FAILED, EnumSet.noneOf(CollectionTaskStatus.class)));

    public void validateTransition(CollectionTaskStatus current, CollectionTaskStatus next) {
        if (current == next)
            return; // Idempotent same-state is fine in service layer usually, or handle there

        Set<CollectionTaskStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current,
                EnumSet.noneOf(CollectionTaskStatus.class));
        if (!allowed.contains(next)) {
            throw new IllegalStateException("Invalid status transition from " + current + " to " + next);
        }
    }
}
