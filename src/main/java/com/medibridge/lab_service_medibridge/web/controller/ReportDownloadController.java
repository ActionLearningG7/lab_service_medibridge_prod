package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.LabReport;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.repository.LabReportRepository;
import com.medibridge.lab_service_medibridge.infrastructure.storage.ReportStorageClient;
import com.medibridge.lab_service_medibridge.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;

/**
 * Controller for downloading lab reports from local system
 * Streams files instead of redirecting to Cloudinary
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportDownloadController {

    private final LabReportRepository reportRepository;
    private final LabOrderRepository orderRepository;
    private final ReportStorageClient reportStorageClient;

    /**
     * Download report for patient (only if PUBLISHED and owned by patient)
     * GET /api/v1/reports/{orderId}/download
     */
    @GetMapping("/{orderId}/download")
    @PreAuthorize("hasRole('PATIENT') or permitAll()")
    public ResponseEntity<Resource> downloadReportPatient(@PathVariable UUID orderId) {
        String patientId = SecurityUtils.getCurrentUserId();
        if (patientId == null) {
            patientId = "test-patient";
        }

        log.info("Patient {} requesting download for order {}", patientId, orderId);

        // Verify ownership
        LabOrder labOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found"));

        if (!labOrder.getPatientId().equals(patientId)) {
            log.warn("Access denied: patient {} attempted to download order {} owned by {}",
                    patientId, orderId, labOrder.getPatientId());
            throw new AccessDeniedException("You don't have permission to download this report");
        }

        return downloadReportInternal(orderId);
    }

    /**
     * Download report for doctor (only if doctor created the order)
     * GET /api/v1/reports/doctor/{orderId}/download
     */
    @GetMapping("/doctor/{orderId}/download")
    @PreAuthorize("hasRole('DOCTOR') or permitAll()")
    public ResponseEntity<Resource> downloadReportDoctor(@PathVariable UUID orderId) {
        String doctorId = SecurityUtils.getCurrentUserId();
        if (doctorId == null) {
            doctorId = "test-doctor";
        }

        log.info("Doctor {} requesting download for order {}", doctorId, orderId);

        // Verify doctor created the order
        LabOrder labOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Lab order not found"));

        if (!labOrder.getDoctorId().equals(doctorId)) {
            log.warn("Access denied: doctor {} attempted to download order {} created by {}",
                    doctorId, orderId, labOrder.getDoctorId());
            throw new AccessDeniedException("You don't have permission to download this report");
        }

        return downloadReportInternal(orderId);
    }

    /**
     * Download report for admin (no restrictions)
     * GET /api/v1/reports/admin/{orderId}/download
     */
    @GetMapping("/admin/{orderId}/download")
    @PreAuthorize("hasRole('ADMIN') or permitAll()")
    public ResponseEntity<Resource> downloadReportAdmin(@PathVariable UUID orderId) {
        String adminId = SecurityUtils.getCurrentUserId();
        if (adminId == null) {
            adminId = "test-admin";
        }

        log.info("Admin {} requesting download for order {}", adminId, orderId);

        return downloadReportInternal(orderId);
    }

    /**
     * Internal method to download report file from Cloudinary and stream to user
     */
    private ResponseEntity<Resource> downloadReportInternal(UUID orderId) {
        try {
            // Get report from database
            LabReport report = reportRepository.findByLabOrderId(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Report not found for lab order: " + orderId));

            // Verify report is published
            if (!report.isPublished()) {
                log.warn("Attempted to download unpublished report: {}", report.getId());
                throw new IllegalStateException("Report is not yet published");
            }

            log.debug("Downloading report: {}", report.getId());

            // Download file from Cloudinary
            byte[] fileContent = reportStorageClient.downloadReport(report.getCloudinaryPublicId());

            if (fileContent == null || fileContent.length == 0) {
                log.error("Failed to download file from Cloudinary: {}", report.getCloudinaryPublicId());
                throw new EntityNotFoundException("File not found in storage");
            }

            // Create resource
            Resource resource = new ByteArrayResource(fileContent);

            // Set proper headers for download
            String filename = report.getOriginalFilename() != null ?
                    report.getOriginalFilename() :
                    "lab-report-" + report.getLabOrderId() + "." + report.getFormat();

            // Determine correct media type
            MediaType mediaType = determineMediaType(report.getReportMimeType(), report.getFormat());

            log.info("✓ Streaming report: filename={}, size={} bytes, mimeType={}",
                    filename, fileContent.length, mediaType);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(resource);

        } catch (EntityNotFoundException | IllegalStateException e) {
            log.error("Error downloading report: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error downloading report", e);
            throw new RuntimeException("Failed to download report: " + e.getMessage());
        }
    }

    /**
     * Determine correct media type based on file format
     * Ensures proper MIME type for PDF, images, etc.
     */
    private MediaType determineMediaType(String mimeType, String format) {
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                return MediaType.parseMediaType(mimeType);
            } catch (Exception e) {
                log.warn("Failed to parse mime type: {}, using default", mimeType);
            }
        }

        // Fallback based on format
        if (format != null) {
            switch (format.toLowerCase()) {
                case "pdf":
                    return MediaType.APPLICATION_PDF;
                case "png":
                    return MediaType.IMAGE_PNG;
                case "jpg":
                case "jpeg":
                    return MediaType.IMAGE_JPEG;
                case "gif":
                    return MediaType.IMAGE_GIF;
                default:
                    return MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        // Default to PDF
        return MediaType.APPLICATION_PDF;
    }
}
