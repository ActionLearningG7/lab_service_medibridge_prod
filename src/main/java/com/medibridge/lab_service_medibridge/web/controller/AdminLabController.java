package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.service.LabReportService;
import com.medibridge.lab_service_medibridge.service.CollectionTaskService;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import com.medibridge.lab_service_medibridge.web.dto.LabReportResponse;
import com.medibridge.lab_service_medibridge.web.dto.ReportPublishRequest;

import com.medibridge.lab_service_medibridge.web.mapper.LabOrderMapper;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/lab")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class AdminLabController {

    private final LabReportService reportService;
    private final LabOrderRepository orderRepository;
    private final CollectionTaskRepository taskRepository;
    private final CollectionTaskService taskService;
    private final LabOrderMapper mapper;

    /**
     * Upload lab report file to Cloudinary
     *
     * POST /api/v1/admin/lab-orders/{orderId}/report/upload
     * Content-Type: multipart/form-data
     *
     * @param orderId Lab order UUID
     * @param file    Report file (PDF or image)
     * @return LabReportResponse with metadata (URL not included for DRAFT)
     */
    @PostMapping(value = "/lab-orders/{orderId}/report/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or permitAll()")
    public ResponseEntity<LabReportResponse> uploadReport(
            @PathVariable UUID orderId,
            @RequestParam("file") MultipartFile file) {

        String uploadedBy = SecurityUtils.getCurrentUserId();
        if (uploadedBy == null) {
            uploadedBy = "test-admin";
        }
        LabReportResponse response = reportService.uploadReport(orderId, file, uploadedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Publish lab report (make it accessible to patient/doctor)
     *
     * POST /api/v1/admin/lab-orders/{orderId}/report/publish
     *
     * @param orderId Lab order UUID
     * @return LabReportResponse with secure URL included
     */
    @PostMapping("/lab-orders/{orderId}/report/publish")
    @PreAuthorize("hasRole('ADMIN') or permitAll()")
    public ResponseEntity<LabReportResponse> publishReportNew(@PathVariable UUID orderId) {
        String publishedBy = SecurityUtils.getCurrentUserId();
        if (publishedBy == null) {
            publishedBy = "test-admin";
        }
        LabReportResponse response = reportService.publishReportNew(orderId, publishedBy);

        return ResponseEntity.ok(response);
    }

    /**
     * Get report for admin (any status)
     *
     * GET /api/v1/admin/lab-orders/{orderId}/report
     *
     * @param orderId Lab order UUID
     * @return LabReportResponse
     */
    @GetMapping("/lab-orders/{orderId}/report")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabReportResponse> getReport(@PathVariable UUID orderId) {
        LabReportResponse response = reportService.getReportForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Legacy endpoint for backward compatibility
     */
    @PostMapping("/orders/{orderId}/publish-report")
    @PreAuthorize("hasRole('ADMIN') or permitAll()")
    public ResponseEntity<Void> publishReport(@PathVariable String orderId,
            @RequestBody @Valid ReportPublishRequest request) {
        reportService.publishReport(orderId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<LabOrderResponse>> getAllOrders(Pageable pageable) {
        Page<LabOrder> page = orderRepository.findAll(pageable);
        return ResponseEntity.ok(page.map(mapper::toResponse));
    }

    /**
     * POST /api/v1/admin/lab/orders/{orderId}/create-collection-task
     * Create a collection task from a lab order and assign to phlebotomist
     *
     * @param orderId Lab order UUID
     * @param phlebotomistId Phlebotomist user ID
     * @return Created CollectionTask details
     */
    @PostMapping("/orders/{orderId}/create-collection-task")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCollectionTask(
            @PathVariable UUID orderId,
            @RequestParam String phlebotomistId) {

        // Get the lab order
        LabOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found: " + orderId));

        // Check if task already exists
        if (taskRepository.findByLabOrderId(orderId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Collection task already exists for this order",
                "orderId", orderId
            ));
        }

        // Create the collection task
        taskService.createTaskFromOrder(order);

        // Get the created task
        CollectionTask task = taskRepository.findByLabOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Failed to create collection task"));

        // Assign to phlebotomist
        String adminId = SecurityUtils.getCurrentUserId();
        taskService.assignTask(task.getId(), phlebotomistId, order.getPreferredSlotStart(),
                order.getSpecialInstructions(), adminId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Collection task created and assigned successfully",
            "taskId", task.getId(),
            "orderId", orderId,
            "phlebotomistId", phlebotomistId,
            "status", "ASSIGNED"
        ));
    }
}
