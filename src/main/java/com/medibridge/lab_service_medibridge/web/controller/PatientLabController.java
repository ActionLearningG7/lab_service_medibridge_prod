package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.service.LabOrderService;
import com.medibridge.lab_service_medibridge.service.LabReportService;
import com.medibridge.lab_service_medibridge.service.LabResultService;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderRequest;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import com.medibridge.lab_service_medibridge.web.dto.LabReportResponse;
import com.medibridge.lab_service_medibridge.web.dto.LabResultResponse;
import com.medibridge.lab_service_medibridge.web.dto.TrackingContextResponse;
import com.medibridge.lab_service_medibridge.web.mapper.LabOrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lab-orders")
@RequiredArgsConstructor
public class PatientLabController {

    private final LabOrderService orderService;
    private final LabOrderRepository orderRepository;
    private final LabOrderMapper mapper;
    private final LabReportService reportService;
    private final LabResultService labResultService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT') or permitAll()")
    public ResponseEntity<LabOrderResponse> createOrder(@RequestBody @Valid LabOrderRequest request) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            // For testing/development - use a test patient ID
            patientId = "test-patient-" + System.currentTimeMillis();
        }

        LabOrder order = orderService.createOrder(request, patientId, false);
        return ResponseEntity.ok(mapper.toResponse(order));
    }

    @GetMapping("/me")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<LabOrderResponse>> getMyOrders(Pageable pageable) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            // For testing/development - return sample data
            patientId = "test-patient";
        }

        Page<LabOrder> page = orderRepository.findByPatientId(patientId, pageable);
        return ResponseEntity.ok(page.map(mapper::toResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabOrderResponse> getOrder(@PathVariable String id) {
        LabOrder order = orderService.getOrder(id);

        String userId = SecurityUtils.getCurrentUserId();
        // For development, skip ownership check if no user is authenticated
        if (userId != null) {
            boolean isOwner = userId.equals(order.getPatientId());
            boolean isAssignedDoctor = userId.equals(order.getDoctorId());

            if (!isOwner && !isAssignedDoctor) {
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.ok(mapper.toResponse(order));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        LabOrder order = orderService.getOrder(id);
        String userId = SecurityUtils.getCurrentUserId();

        // For development, allow if no user authenticated
        if (userId != null && !order.getPatientId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get lab report for patient's order (only if PUBLISHED)
     *
     * GET /api/v1/lab-orders/{orderId}/report
     *
     * @param orderId Lab order UUID
     * @return LabReportResponse with secure URL
     */
    @GetMapping("/{orderId}/report")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabReportResponse> getMyReport(@PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        LabReportResponse response = reportService.getReportForPatient(orderId, patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get lab results for an order
     * GET /api/v1/lab-orders/{orderId}/results
     */
    @GetMapping("/{orderId}/results")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getLabResults(@PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        // Verify order belongs to patient
        LabOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!order.getPatientId().equals(patientId)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<LabResultResponse> results = labResultService.getLabResults(orderId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all lab reports for the current patient
     * GET /api/v1/lab-orders/reports/me
     */
    @GetMapping("/reports/me")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getMyReports() {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            // For testing/development - use a test patient ID
            patientId = "test-patient"; // Or handle as unauthorized
        }

        // Fetch all orders for this patient
        List<LabOrder> orders = orderRepository.findByPatientId(patientId);

        // Filter orders that have results or completed status
        // AND map them to a structure suitable for the "My Reports" page
        // This logic could be moved to a service
        List<LabOrderResponse> reports = orders.stream()
                .filter(order -> order
                        .getStatus() == com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus.RESULT_UPLOADED
                        ||
                        order.getStatus() == com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus.COMPLETED
                        ||
                        order.getStatus() == com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus.REPORT_PUBLISHED)
                .map(order -> {
                    LabOrderResponse response = mapper.toResponse(order);
                    // Enrich with results
                    List<LabResultResponse> results = labResultService.getLabResults(order.getId());
                    response.setResults(results);
                    return response;
                })
                // Only include orders that actually have results uploaded
                .filter(response -> response.getResults() != null && !response.getResults().isEmpty())
                .toList();

        return ResponseEntity.ok(reports);
    }

    // -- TRACKING --

    private final TrackingFacade trackingFacade; // Constructor injection assumes Lombok RequiredArgsConstructor updates

    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("permitAll()")
    public ResponseEntity<com.medibridge.lab_service_medibridge.domain.model.TrackingStateResponse> getTracking(
            @PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        return trackingFacade.getTrackingState(orderId, patientId, false);
    }

    @GetMapping("/{orderId}/collection-task")
    @PreAuthorize("permitAll()")
    public ResponseEntity<com.medibridge.lab_service_medibridge.domain.model.CollectionTaskResponse> getCollectionTask(
            @PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        return trackingFacade.getTaskDetails(orderId, patientId);
    }

    /**
     * Get tracking context (hospital coords + destination coords + status + phleb
     * summary)
     * For patient live tracking map
     */
    @GetMapping("/{orderId}/tracking-context")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TrackingContextResponse> getTrackingContext(@PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        return trackingFacade.getTrackingContext(orderId, patientId, false);
    }
}
