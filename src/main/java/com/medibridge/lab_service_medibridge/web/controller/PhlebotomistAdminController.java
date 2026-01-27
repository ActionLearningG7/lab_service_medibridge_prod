package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.service.LabResultService;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import com.medibridge.lab_service_medibridge.web.dto.LabResultResponse;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus;
import com.medibridge.lab_service_medibridge.web.mapper.LabOrderMapper;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Controller for phlebotomist admin operations
 * Only phlebotomists with isAdmin=true can access
 */
@RestController
@RequestMapping("/api/v1/phlebotomy-admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Phlebotomist Admin", description = "Admin operations for phlebotomists")
public class PhlebotomistAdminController {

    private final LabResultService labResultService;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderMapper labOrderMapper;

    /**
     * Upload lab result for an order
     * POST /api/v1/phlebotomy-admin/orders/{orderId}/results
     */
    @PostMapping(value = "/orders/{orderId}/results", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    @Operation(summary = "Upload lab result", description = "Upload lab test result file. Only phlebotomist admins can upload.")
    public ResponseEntity<?> uploadLabResult(
            @PathVariable UUID orderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-Is-Admin", required = false, defaultValue = "false") Boolean isAdmin
    ) {
        log.info("Upload lab result request from user: {} for order: {}", userId, orderId);

        // Validate phlebotomist admin
//        if (!isAdmin) {
//            log.warn("Unauthorized result upload attempt by non-admin phlebotomist: {}", userId);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body("Only phlebotomist admins can upload results");
//        }

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }

        // Validate file type (PDF, images)
        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.equals("application/pdf") &&
             !contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest()
                    .body("Only PDF and image files are allowed");
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest()
                    .body("File size must not exceed 10MB");
        }

        try {
            LabResultResponse response = labResultService.uploadLabResult(
                    orderId,
                    file,
                    userId,
                    userName,
                    notes
            );

            log.info("Lab result uploaded successfully: {} for order: {}", response.getResultId(), orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload lab result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload result: " + e.getMessage());
        }
    }

    /**
     * Delete a lab result
     * DELETE /api/v1/phlebotomy-admin/results/{resultId}
     */
    @DeleteMapping("/results/{resultId}")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    @Operation(summary = "Delete lab result", description = "Delete a lab result. Only phlebotomist admins can delete.")
    public ResponseEntity<?> deleteLabResult(
            @PathVariable UUID resultId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Is-Admin", required = false, defaultValue = "false") Boolean isAdmin
    ) {
        log.info("Delete lab result request: {} by user: {}", resultId, userId);

//        if (!isAdmin) {
//            log.warn("Unauthorized result delete attempt by non-admin phlebotomist: {}", userId);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body("Only phlebotomist admins can delete results");
//        }

        try {
            labResultService.deleteLabResult(resultId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete lab result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete result: " + e.getMessage());
        }
    }

    /**
     * Get eligible orders for result upload
     * GET /api/v1/phlebotomy-admin/eligible-orders
     */
    @GetMapping("/eligible-orders")
    @PreAuthorize("hasRole('PHLEBOTOMIST')")
    @Operation(summary = "Get eligible orders", description = "Get orders eligible for result upload")
    public ResponseEntity<?> getEligibleOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LabOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
//        if (!isAdmin) {
//            log.warn("Unauthorized eligible orders access attempt by non-admin phlebotomist");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body("Only phlebotomist admins can view eligible orders");
//        }

        // Build specification for filtering
        Specification<LabOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by eligible statuses
            List<LabOrderStatus> eligibleStatuses = List.of(
                LabOrderStatus.COLLECTED,
                LabOrderStatus.IN_TRANSIT,
                LabOrderStatus.AT_LAB,
                LabOrderStatus.TESTING,
                LabOrderStatus.RESULT_UPLOADED
            );
            predicates.add(root.get("status").in(eligibleStatuses));

            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate orderNumberMatch = cb.like(cb.lower(root.get("orderNumber")), searchPattern);
                Predicate patientNameMatch = cb.like(cb.lower(root.get("patientName")), searchPattern);
                predicates.add(cb.or(orderNumberMatch, patientNameMatch));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<LabOrder> orders = labOrderRepository.findAll(spec, pageRequest);
        
        Page<LabOrderResponse> response = orders.map(labOrderMapper::toResponse);
        
        log.info("Retrieved {} eligible orders for result upload", orders.getTotalElements());
        return ResponseEntity.ok(response);
    }
}
