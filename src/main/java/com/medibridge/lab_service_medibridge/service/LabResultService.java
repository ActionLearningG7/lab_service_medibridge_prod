package com.medibridge.lab_service_medibridge.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.LabResult;
import com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus;
import com.medibridge.lab_service_medibridge.domain.repository.LabResultRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.web.dto.LabResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing lab results and file uploads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final Cloudinary cloudinary;

    /**
     * Upload lab result file to Cloudinary and save metadata
     */
    @Transactional
    public LabResultResponse uploadLabResult(
            UUID orderId,
            MultipartFile file,
            String uploadedBy,
            String uploadedByName,
            String notes
    ) {
        log.info("Uploading lab result for order: {}", orderId);

        // Validate order exists
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Lab order not found: " + orderId));

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Upload to Cloudinary
        Map<String, Object> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "lab-results",
                    "resource_type", "auto",
                    "use_filename", true,
                    "unique_filename", true
            ));
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }

        String fileUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        // Mark previous results as not latest
        List<LabResult> existingResults = labResultRepository.findByLabOrderId(orderId);
        existingResults.forEach(result -> result.setIsLatest(false));
        labResultRepository.saveAll(existingResults);

        // Get next version number
        Integer nextVersion = labResultRepository.findNextVersionForOrder(orderId);

        // Save result metadata
        LabResult labResult = LabResult.builder()
                .labOrder(order)
                .fileUrl(fileUrl)
                .cloudinaryPublicId(publicId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .version(nextVersion)
                .isLatest(true)
                .notes(notes)
                .uploadedBy(uploadedBy)
                .uploadedByName(uploadedByName)
                .deleted(false)
                .build();

        labResult = labResultRepository.save(labResult);

        // Update order status
        if (order.getStatus() != LabOrderStatus.RESULT_UPLOADED) {
            order.setStatus(LabOrderStatus.RESULT_UPLOADED);
            order.setUpdatedAt(LocalDateTime.now());
            labOrderRepository.save(order);
        }

        log.info("Lab result uploaded successfully: {} for order: {}", labResult.getResultId(), orderId);

        return mapToResponse(labResult);
    }

    /**
     * Get all results for a lab order
     */
    @Transactional(readOnly = true)
    public List<LabResultResponse> getLabResults(UUID orderId) {
        return labResultRepository.findByLabOrderId(orderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get latest result for a lab order
     */
    @Transactional(readOnly = true)
    public LabResultResponse getLatestLabResult(UUID orderId) {
        return labResultRepository.findLatestByLabOrderId(orderId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    /**
     * Delete a lab result (soft delete)
     */
    @Transactional
    public void deleteLabResult(UUID resultId, String deletedBy) {
        LabResult result = labResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Lab result not found: " + resultId));

        result.softDelete();
        labResultRepository.save(result);

        // If this was the latest, mark previous version as latest
        if (result.getIsLatest()) {
            List<LabResult> remainingResults = labResultRepository.findByLabOrderId(result.getLabOrder().getId());
            if (!remainingResults.isEmpty()) {
                LabResult previousLatest = remainingResults.get(0);
                previousLatest.setIsLatest(true);
                labResultRepository.save(previousLatest);
            }
        }

        log.info("Lab result deleted: {} by: {}", resultId, deletedBy);
    }

    /**
     * Map LabResult entity to response DTO
     */
    private LabResultResponse mapToResponse(LabResult result) {
        return LabResultResponse.builder()
                .resultId(result.getResultId())
                .labOrderId(result.getLabOrder().getId())
                .orderNumber(result.getLabOrder().getOrderNumber())
                .fileUrl(result.getFileUrl())
                .fileName(result.getFileName())
                .fileType(result.getFileType())
                .fileSize(result.getFileSize())
                .version(result.getVersion())
                .isLatest(result.getIsLatest())
                .notes(result.getNotes())
                .uploadedBy(result.getUploadedBy())
                .uploadedByName(result.getUploadedByName())
                .uploadedAt(result.getUploadedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
