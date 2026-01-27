package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import com.medibridge.lab_service_medibridge.domain.model.*;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.service.CollectionTaskService;
import com.medibridge.lab_service_medibridge.service.RedisTrackingService;
import com.medibridge.lab_service_medibridge.util.LabMapper;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import com.medibridge.lab_service_medibridge.web.dto.NavigationContextResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Phlebotomist Task Management Controller
 * Handles logistics workflow: ACCEPT -> EN_ROUTE -> ARRIVE -> COLLECT ->
 * DELIVER -> COMPLETE
 */
@RestController
@RequestMapping("/api/v1/phlebotomy")
@RequiredArgsConstructor
@Slf4j
public class PhlebotomistTaskController {

    private final CollectionTaskService taskService;
    private final CollectionTaskRepository taskRepository;
    private final LabOrderRepository labOrderRepository;
    private final RedisTrackingService trackingService;
    private final LabMapper mapper;
    private final com.medibridge.lab_service_medibridge.web.controller.TrackingFacade trackingFacade;

    /**
     * Get assigned tasks
     * Query param status: ASSIGNED, ACCEPTED, etc.
     */
    @GetMapping("/tasks/me")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<List<PhlebotomistTaskSummaryResponse>> getMyTasks(
            @RequestParam(required = false) List<CollectionTaskStatus> status) {

        String phlebId = SecurityUtils.getCurrentUserId();

        List<CollectionTaskStatus> statuses = status != null && !status.isEmpty() ? status
                : List.of(CollectionTaskStatus.ASSIGNED, CollectionTaskStatus.ACCEPTED, CollectionTaskStatus.EN_ROUTE,
                        CollectionTaskStatus.ARRIVED, CollectionTaskStatus.SAMPLES_COLLECTED,
                        CollectionTaskStatus.IN_TRANSIT);

        List<CollectionTask> tasks = taskRepository.findActiveTasksForPhlebotomist(phlebId, statuses);

        List<PhlebotomistTaskSummaryResponse> response = tasks.stream().map(task -> {
            // Need patient address details from LabOrder
            // In a real optimized system, we'd use a DTO projection or fetch join.
            // For MVP, simplistic fetch.
            LabOrder order = labOrderRepository.findById(task.getLabOrderId()).orElse(null);
            String address = order != null ? order.getAddressLine1() : "";
            String city = order != null ? order.getCity() : "";
            String zip = order != null ? order.getPostalCode() : "";

            // Assuming we might have geocoordinates in Order or Address
            Double lat = order != null && order.getDeliveryLat() != null ? order.getDeliveryLat().doubleValue() : null;
            Double lng = order != null && order.getDeliveryLng() != null ? order.getDeliveryLng().doubleValue() : null;

            return mapper.toPhlebSummary(task, address, city, zip, lat, lng);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific task details
     */
    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<CollectionTaskResponse> getTaskDetail(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        CollectionTask task = taskRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Task not found"));

        // Security check
        if (!phlebId.equals(task.getPhlebotomistId())) {
            return ResponseEntity.status(403).build();
        }

        CollectionTaskResponse response = mapper.toTaskResponse(task);

        // Populate test details from LabOrder
        // Populate test details from LabOrder
        labOrderRepository.findById(task.getLabOrderId()).ifPresent(order -> {
            List<CollectionTaskResponse.TestDetail> tests = order.getItems().stream()
                    .map(item -> CollectionTaskResponse.TestDetail.builder()
                            .id(item.getId())
                            .name(item.getTestName())
                            .code(item.getTestCode())
                            .build())
                    .collect(Collectors.toList());
            response.setTests(tests);

            // Populate Patient Data
            // Populate Patient Data
            response.setPatientName("Patient " + order.getPatientId().substring(0, 8));
            response.setPatientPhone(order.getContactPhone());

            // Populate Address Data
            response.setAddress(CollectionTaskResponse.AddressDetail.builder()
                    .line1(order.getAddressLine1())
                    .line2(null)
                    .city(order.getCity())
                    .state(order.getState())
                    .zipCode(order.getPostalCode())
                    .coordinates(CollectionTaskResponse.Coordinates.builder()
                            .lat(order.getDeliveryLat() != null ? order.getDeliveryLat().doubleValue() : null)
                            .lng(order.getDeliveryLng() != null ? order.getDeliveryLng().doubleValue() : null)
                            .build())
                    .build());

            // Populate Instructions
            // Logic to determine instructions based on tests (e.g., fasting)
            boolean fasting = order.getItems().stream()
                    .anyMatch(item -> item.getTestName().toLowerCase().contains("fasting")
                            || item.getTestName().toLowerCase().contains("lipid")
                            || item.getTestName().toLowerCase().contains("glucose"));
            response.setInstructions(CollectionTaskResponse.InstructionDetail.builder()
                    .fastingInstructions(fasting ? "Patient must be fasting for 8-12 hours." : "No fasting required.")
                    .generalNotes(order.getSpecialInstructions())
                    .build());
        });

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{id}/accept")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> acceptTask(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        taskService.transition(id, CollectionTaskStatus.ACCEPTED, phlebId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/en-route")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> markEnRoute(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        taskService.transition(id, CollectionTaskStatus.EN_ROUTE, phlebId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/arrive")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> markArrived(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        taskService.transition(id, CollectionTaskStatus.ARRIVED, phlebId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/collect-samples")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> collectSamples(@PathVariable UUID id,
            @RequestBody @Valid SampleCollectionRequest request) {
        String phlebId = SecurityUtils.getCurrentUserId();
        taskService.collectSamples(id, request, phlebId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/deliver-to-lab")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> deliverToLab(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        // Skip IN_TRANSIT for MVP, go straight to DELIVERED
        taskService.transition(id, CollectionTaskStatus.DELIVERED_TO_LAB, phlebId);

        // Auto-complete task upon delivery? Or separate step?
        // Requirement says: DELIVERED -> COMPLETED
        taskService.transition(id, CollectionTaskStatus.COMPLETED, phlebId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> completeTask(@PathVariable UUID id) {
        // Fallback manual complete
        String phlebId = SecurityUtils.getCurrentUserId();
        taskService.transition(id, CollectionTaskStatus.COMPLETED, phlebId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{id}/location")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> updateLocation(@PathVariable UUID id,
            @RequestBody @Valid LocationUpdateRequest request) {
        String phlebId = SecurityUtils.getCurrentUserId();

        // Verify task existence and ownership to avoid spam
        CollectionTask task = taskRepository.findById(id).orElse(null);
        if (task == null || !task.getPhlebotomistId().equals(phlebId)) {
            return ResponseEntity.notFound().build();
        }

        // Only allow updates if task is active
        if (task.getStatus() == CollectionTaskStatus.COMPLETED || task.getStatus() == CollectionTaskStatus.CANCELLED) {
            return ResponseEntity.badRequest().build();
        }

        trackingService.updateLocation(id, task.getLabOrderId(), phlebId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/availability")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<Void> updateAvailability(@RequestBody AvailabilityRequest request) {
        // Toggle online status in Redis for "Nearest Phleb" feature
        // Implementation pending: update geo set or remove
        return ResponseEntity.ok().build();
    }

    /**
     * Get navigation context for phlebotomist navigation view
     * Returns destination coords + hospital coords + patient info
     */
    @GetMapping("/tasks/{id}/navigation-context")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    public ResponseEntity<NavigationContextResponse> getNavigationContext(@PathVariable UUID id) {
        String phlebId = SecurityUtils.getCurrentUserId();
        return trackingFacade.getNavigationContext(id, phlebId);
    }

    @lombok.Data
    public static class AvailabilityRequest {
        private boolean online;
    }
}
