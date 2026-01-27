package com.medibridge.lab_service_medibridge.event.dto;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionTaskStatusChangedEvent {
    private UUID taskId;
    private CollectionTaskStatus oldStatus;
    private CollectionTaskStatus newStatus;
    private LocalDateTime timestamp;
}
