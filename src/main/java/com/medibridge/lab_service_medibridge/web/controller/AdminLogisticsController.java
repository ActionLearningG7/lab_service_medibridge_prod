package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import com.medibridge.lab_service_medibridge.domain.model.AssignTaskRequest;
import com.medibridge.lab_service_medibridge.domain.model.CollectionTaskResponse;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.service.CollectionTaskService;
import com.medibridge.lab_service_medibridge.util.LabMapper;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/lab/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogisticsController {

    private final CollectionTaskService taskService;
    private final CollectionTaskRepository taskRepository;
    private final LabMapper mapper;

    @GetMapping
    public ResponseEntity<Page<CollectionTaskResponse>> getTasks(
            @RequestParam(required = false) CollectionTaskStatus status,
            Pageable pageable) {

        Page<CollectionTask> page = status != null
                ? taskRepository.findByStatus(status, pageable)
                : taskRepository.findAll(pageable);

        return ResponseEntity.ok(page.map(mapper::toTaskResponse));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CollectionTaskResponse> getTask(@PathVariable UUID taskId) {
        return taskRepository.findById(taskId)
                .map(mapper::toTaskResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{taskId}/assign")
    public ResponseEntity<Void> assignTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid AssignTaskRequest request) {

        String adminId = SecurityUtils.getCurrentUserId();
        taskService.assignTask(
                taskId,
                request.getPhlebotomistId(),
                request.getScheduledAt(),
                request.getNotesForPhlebotomist(),
                adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/reassign")
    public ResponseEntity<Void> reassignTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid AssignTaskRequest request) {
        // Reuse assign logic, state machine handles transition (ASSIGNED -> ASSIGNED
        // allowed)
        return assignTask(taskId, request);
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Void> cancelTask(
            @PathVariable UUID taskId,
            @RequestParam(required = false) String reason) {

        String adminId = SecurityUtils.getCurrentUserId();
        // Since transition() enforces strict flows, simple CANCEL is not directly
        // exposed as generic transition in service yet
        // We'll call transition with CANCELLED

        // TODO: Enhancement to pass reason to service
        taskService.transition(taskId, CollectionTaskStatus.CANCELLED, adminId);
        return ResponseEntity.ok().build();
    }
}
