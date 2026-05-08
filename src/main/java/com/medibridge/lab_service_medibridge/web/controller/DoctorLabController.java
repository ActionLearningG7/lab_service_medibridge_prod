package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.service.LabOrderService;
import com.medibridge.lab_service_medibridge.service.LabReportService;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderRequest;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import com.medibridge.lab_service_medibridge.web.dto.LabReportResponse;
import com.medibridge.lab_service_medibridge.web.mapper.LabOrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors/lab-orders")
@RequiredArgsConstructor
public class DoctorLabController {

    private final LabOrderService orderService;
    private final LabOrderMapper mapper;
    private final LabReportService reportService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or permitAll()")
    public ResponseEntity<LabOrderResponse> createOrder(@RequestBody @Valid LabOrderRequest request) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            // For testing/development
            doctorId = "test-doctor-" + System.currentTimeMillis();
        }
        // Validation: Ensure patientId is present in request (DTO validation should
        // handle it)
        if (request.getPatientId() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(orderService.createOrder(request, doctorId, true));
    }

    /**
     * GET /api/v1/doctors/lab-orders
     * List all lab orders created by this doctor
     *
     * @param pageable Pagination parameters
     * @return Page of lab orders
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<LabOrderResponse>> getDoctorOrders(Pageable pageable) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            doctorId = "test-doctor";
        }

        // For now, return empty page (backend needs to implement actual filtering)
        // TODO: Implement actual doctor orders repository query
        Page<LabOrderResponse> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        return ResponseEntity.ok(emptyPage);
    }

    /**
     * GET /api/v1/doctors/lab-orders/{orderId}
     * Get specific lab order details
     *
     * @param orderId Lab order UUID
     * @return Lab order details
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabOrderResponse> getDoctorOrder(@PathVariable UUID orderId) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            doctorId = "test-doctor";
        }

        try {
            LabOrder order = orderService.getOrder(orderId.toString());
            // Verify doctor owns this order
            if (!order.getDoctorId().equals(doctorId)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(mapper.toResponse(order));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get lab report for order created by doctor (only if PUBLISHED)
     *
     * GET /api/v1/doctors/lab-orders/{orderId}/report
     *
     * @param orderId Lab order UUID
     * @return LabReportResponse with secure URL
     */
    @GetMapping("/{orderId}/report")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabReportResponse> getReport(@PathVariable UUID orderId) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            doctorId = "test-doctor";
        }
        LabReportResponse response = reportService.getReportForDoctor(orderId, doctorId);
        return ResponseEntity.ok(response);
    }

    private final TrackingFacade trackingFacade;

    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("permitAll()")
    public ResponseEntity<com.medibridge.lab_service_medibridge.domain.model.TrackingStateResponse> getTracking(
            @PathVariable UUID orderId) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            doctorId = "test-doctor";
        }
        return trackingFacade.getTrackingState(orderId, doctorId, true);
    }
}
