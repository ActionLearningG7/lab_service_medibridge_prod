package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.*;
import com.medibridge.lab_service_medibridge.domain.enums.*;
import com.medibridge.lab_service_medibridge.exception.CloudStorageException;
import com.medibridge.lab_service_medibridge.exception.InvalidFileException;
import com.medibridge.lab_service_medibridge.infrastructure.storage.ReportStorageClient;
import com.medibridge.lab_service_medibridge.infrastructure.storage.ReportUploadResult;
import com.medibridge.lab_service_medibridge.repository.*;
import com.medibridge.lab_service_medibridge.web.dto.LabReportResponse;
import com.medibridge.lab_service_medibridge.web.dto.ReportPublishRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for lab report management with Cloudinary storage
 *
 * Security:
 * - Only ADMIN/LAB can upload and publish reports
 * - Patients can only view their own PUBLISHED reports
 * - Doctors can only view reports for orders they created
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LabReportService {

    private final LabReportRepository reportRepository;
    private final LabOrderRepository orderRepository;
    private final com.medibridge.lab_service_medibridge.event.DomainEventPublisher eventPublisher;
    private final AuditService auditService;
    private final ReportStorageClient reportStorageClient;

    /**
     * Upload lab report file (ADMIN/LAB only)
     * Creates LabReport entity in DRAFT status
     */
    @Transactional
    public LabReportResponse uploadReport(UUID labOrderId, MultipartFile file, String uploadedBy) {
        log.info("Uploading report for lab order: {}, uploaded by: {}", labOrderId, uploadedBy);

        // Verify lab order exists
        LabOrder labOrder = orderRepository.findById(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found: " + labOrderId));

        // Check if report already exists
        Optional<LabReport> existing = reportRepository.findByLabOrderId(labOrderId);
        if (existing.isPresent() && existing.get().getStatus() == LabReportStatus.PUBLISHED) {
            throw new IllegalStateException("Report already published for this lab order");
        }

        // Validate file
        reportStorageClient.validateFile(file);

        try {
            // Upload to Cloudinary
            String labOrderNumber = labOrder.getOrderNumber();
            String patientId = labOrder.getPatientId();

            ReportUploadResult uploadResult = reportStorageClient.uploadReport(
                    file,
                    labOrderNumber,
                    patientId);

            // Create or update LabReport entity
            LabReport report = existing.orElse(new LabReport());
            report.setLabOrderId(labOrderId);
            report.setStatus(LabReportStatus.DRAFT);
            report.setReportUrl(uploadResult.getSecureUrl());
            report.setCloudinaryPublicId(uploadResult.getPublicId());
            report.setReportMimeType(uploadResult.getMimeType());
            report.setReportSizeBytes(uploadResult.getBytes());
            report.setChecksum(uploadResult.getChecksum());
            report.setOriginalFilename(uploadResult.getOriginalFilename());
            report.setResourceType(uploadResult.getResourceType());
            report.setFormat(uploadResult.getFormat());

            report = reportRepository.save(report);

            auditService.logAction(AuditAction.CREATE, "LabReport", report.getId().toString(),
                    "Report uploaded for order: " + labOrderNumber);

            eventPublisher.publishReportUploaded(labOrderId, report.getId());

            log.info("✓ Report uploaded successfully: reportId={}, orderId={}",
                    report.getId(), labOrderId);

            return mapToResponse(report, false); // Don't include URL for DRAFT

        } catch (InvalidFileException | CloudStorageException e) {
            log.error("Failed to upload report for order {}: {}", labOrderId, e.getMessage());
            throw e;
        }
    }

    /**
     * Publish lab report (ADMIN/LAB only) - New Cloudinary version
     * Changes status from DRAFT to PUBLISHED
     */
    @Transactional
    public LabReportResponse publishReportNew(UUID labOrderId, String publishedBy) {
        log.info("Publishing report for lab order: {}, published by: {}", labOrderId, publishedBy);

        LabReport report = reportRepository.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found for lab order: " + labOrderId));

        if (report.getStatus() == LabReportStatus.PUBLISHED) {
            throw new IllegalStateException("Report is already published");
        }

        // Update status
        report.setStatus(LabReportStatus.PUBLISHED);
        report.setPublishedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        // Update order status
        LabOrder order = orderRepository.findById(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found"));
        order.setStatus(LabOrderStatus.COMPLETED);
        orderRepository.save(order);

        // Audit and Kafka
        auditService.logAction(AuditAction.PUBLISH, "LabReport", report.getId().toString(), "Report Published");
        eventPublisher.publishReportPublished(order.getId(), report.getId());

        log.info("✓ Report published successfully: reportId={}", report.getId());

        return mapToResponse(report, true); // Include URL for PUBLISHED
    }

    /**
     * Legacy publish method (kept for backward compatibility)
     */
    @Transactional
    public void publishReport(String orderId, ReportPublishRequest request) {
        LabOrder order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        LabReport report = reportRepository.findByLabOrderId(order.getId())
                .orElse(new LabReport());

        report.setLabOrderId(order.getId());
        report.setStatus(LabReportStatus.PUBLISHED);
        report.setReportUrl(request.getReportUrl());
        report.setReportData(request.getReportDataJson());
        report.setPublishedAt(LocalDateTime.now());

        reportRepository.save(report);

        order.setStatus(LabOrderStatus.COMPLETED);
        orderRepository.save(order);

        auditService.logAction(AuditAction.PUBLISH, "LabReport", report.getId().toString(), "Report Published");
        eventPublisher.publishReportPublished(order.getId(), report.getId());
    }

    /**
     * Get report for patient (only if PUBLISHED and owned by patient)
     */
    @Transactional(readOnly = true)
    public LabReportResponse getReportForPatient(UUID labOrderId, String patientId) {
        log.debug("Patient {} requesting report for order {}", patientId, labOrderId);

        LabReport report = reportRepository.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found for lab order: " + labOrderId));

        // Verify ownership
        LabOrder labOrder = orderRepository.findById(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found"));

        if (!labOrder.getPatientId().equals(patientId)) {
            log.warn("Access denied: patient {} attempted to access order {} owned by {}",
                    patientId, labOrderId, labOrder.getPatientId());
            throw new AccessDeniedException("You don't have permission to access this report");
        }

        // Only return PUBLISHED reports
        if (report.getStatus() != LabReportStatus.PUBLISHED) {
            throw new IllegalStateException("Report is not yet published");
        }

        return mapToResponse(report, true);
    }

    /**
     * Get report for doctor (only if doctor created the order)
     */
    @Transactional(readOnly = true)
    public LabReportResponse getReportForDoctor(UUID labOrderId, String doctorId) {
        log.debug("Doctor {} requesting report for order {}", doctorId, labOrderId);

        LabReport report = reportRepository.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found for lab order: " + labOrderId));

        // Verify doctor created the order
        LabOrder labOrder = orderRepository.findById(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found"));

        if (!labOrder.getDoctorId().equals(doctorId)) {
            log.warn("Access denied: doctor {} attempted to access order {} created by {}",
                    doctorId, labOrderId, labOrder.getDoctorId());
            throw new AccessDeniedException("You don't have permission to access this report");
        }

        // Only return PUBLISHED reports
        if (report.getStatus() != LabReportStatus.PUBLISHED) {
            throw new IllegalStateException("Report is not yet published");
        }

        return mapToResponse(report, true);
    }

    /**
     * Get report for admin (no restrictions)
     */
    @Transactional(readOnly = true)
    public LabReportResponse getReportForAdmin(UUID labOrderId) {
        log.debug("Admin requesting report for order {}", labOrderId);

        LabReport report = reportRepository.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found for lab order: " + labOrderId));

        // Admin can view any status
        boolean includeUrl = report.getStatus() == LabReportStatus.PUBLISHED;
        return mapToResponse(report, includeUrl);
    }

    /**
     * Map entity to DTO
     */
    private LabReportResponse mapToResponse(LabReport report, boolean includeUrl) {
        LabReportResponse.LabReportResponseBuilder builder = LabReportResponse.builder()
                .reportId(report.getId())
                .labOrderId(report.getLabOrderId())
                .status(report.getStatus().name())
                .mimeType(report.getReportMimeType())
                .sizeBytes(report.getReportSizeBytes())
                .format(report.getFormat())
                .originalFilename(report.getOriginalFilename())
                .checksum(report.getChecksum())
                .publishedAt(report.getPublishedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt());

        // Only include URL if report is published and requested
        if (includeUrl && report.isPublished()) {
            builder.reportUrl(report.getReportUrl());
        }

        return builder.build();
    }
}
