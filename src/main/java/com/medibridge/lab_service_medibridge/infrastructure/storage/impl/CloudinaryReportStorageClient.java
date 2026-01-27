package com.medibridge.lab_service_medibridge.infrastructure.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.medibridge.lab_service_medibridge.exception.CloudStorageException;
import com.medibridge.lab_service_medibridge.exception.InvalidFileException;
import com.medibridge.lab_service_medibridge.infrastructure.storage.ReportStorageClient;
import com.medibridge.lab_service_medibridge.infrastructure.storage.ReportUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Cloudinary implementation for lab report storage
 *
 * Features:
 * - Uploads PDFs and images to Cloudinary
 * - Organized folder structure: {base}/{year}/{month}/{labOrderNumber}/
 * - Computes SHA-256 checksum for integrity
 * - Returns secure HTTPS URLs only
 * - Healthcare-grade security (no public access)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryReportStorageClient implements ReportStorageClient {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:medibridge/lab-reports}")
    private String baseFolder;

    @Value("${cloudinary.resource-type:auto}")
    private String defaultResourceType;

    @Value("${max.file.size.mb:10}")
    private long maxFileSizeMB;

    @Value("${allowed.mime.types:application/pdf,image/png,image/jpeg,image/jpg}")
    private String allowedMimeTypes;

    private static final List<String> PDF_EXTENSIONS = Arrays.asList("pdf");
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg");

    @Override
    public ReportUploadResult uploadReport(MultipartFile file, String labOrderNumber, String patientId) {
        log.info("Starting upload for lab order: {}, patient: {}", labOrderNumber, patientId);

        // Validate file
        validateFile(file);

        // Compute checksum before upload
        String checksum = computeChecksum(file);
        log.debug("File checksum computed: {}", checksum);

        try {
            // Determine resource type based on file
            String resourceType = determineResourceType(file);

            // Build folder path: base/yyyy/MM/labOrderNumber/
            String folderPath = buildFolderPath(labOrderNumber);

            // Build public ID: lab_{labOrderNumber}_{timestamp}
            String publicId = buildPublicId(labOrderNumber);

            String fullPath = folderPath + "/" + publicId;

            log.info("Uploading to Cloudinary: folder={}, publicId={}, resourceType={}",
                    folderPath, publicId, resourceType);

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderPath,
                            "public_id", publicId,
                            "resource_type", resourceType,
                            "use_filename", false,
                            "unique_filename", true,
                            "overwrite", false,
                            "secure", true,
                            "access_mode", "authenticated", // Requires signed URLs for access
                            "tags", Arrays.asList("lab-report", "patient:" + patientId, "order:" + labOrderNumber)
                    )
            );

            // Extract result
            ReportUploadResult result = ReportUploadResult.builder()
                    .secureUrl((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .resourceType((String) uploadResult.get("resource_type"))
                    .bytes(((Number) uploadResult.get("bytes")).longValue())
                    .format((String) uploadResult.get("format"))
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .checksum(checksum)
                    .createdAt(((Number) uploadResult.getOrDefault("created_at", System.currentTimeMillis() / 1000)).longValue())
                    .build();

            // Add dimensions if image
            if ("image".equals(resourceType)) {
                result.setWidth((Integer) uploadResult.get("width"));
                result.setHeight((Integer) uploadResult.get("height"));
            }

            log.info("✓ Upload successful: publicId={}, size={} bytes, format={}",
                    result.getPublicId(), result.getBytes(), result.getFormat());
            // Never log secure URL with query params

            return result;

        } catch (IOException e) {
            log.error("Failed to upload report for order {}: {}", labOrderNumber, e.getMessage());
            throw new CloudStorageException("Failed to upload report: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteReport(String publicId) {
        log.info("Deleting report with publicId: {}", publicId);

        try {
            // Determine resource type from publicId or try both
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "raw",
                    "invalidate", true
            ));

            String resultStatus = (String) result.get("result");
            boolean deleted = "ok".equals(resultStatus);

            if (deleted) {
                log.info("✓ Report deleted successfully: {}", publicId);
            } else {
                log.warn("Report deletion returned status: {}", resultStatus);
            }

            return deleted;

        } catch (IOException e) {
            log.error("Failed to delete report {}: {}", publicId, e.getMessage());
            throw new CloudStorageException("Failed to delete report: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }

        // Check file size
        long fileSizeBytes = file.getSize();
        long maxSizeBytes = maxFileSizeMB * 1024 * 1024;

        if (fileSizeBytes > maxSizeBytes) {
            throw new InvalidFileException(
                    String.format("File size %d bytes exceeds maximum allowed %d MB",
                            fileSizeBytes, maxFileSizeMB));
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidFileException("File content type is missing");
        }

        List<String> allowed = Arrays.asList(allowedMimeTypes.split(","));
        if (!allowed.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    String.format("File type %s not allowed. Allowed types: %s",
                            contentType, allowedMimeTypes));
        }

        // Check filename extension
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("Filename is missing");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (extension.isEmpty()) {
            throw new InvalidFileException("File has no extension");
        }

        boolean validExtension = PDF_EXTENSIONS.contains(extension) || IMAGE_EXTENSIONS.contains(extension);
        if (!validExtension) {
            throw new InvalidFileException(
                    String.format("File extension .%s not allowed. Allowed: pdf, png, jpg, jpeg", extension));
        }

        log.debug("File validation passed: size={} bytes, type={}, name={}",
                fileSizeBytes, contentType, filename);
    }

    /**
     * Compute SHA-256 checksum of file content
     */
    private String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to compute checksum: {}", e.getMessage());
            throw new CloudStorageException("Failed to compute file checksum", e);
        }
    }

    /**
     * Determine resource type based on file type
     */
    private String determineResourceType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null) {
            return "raw";
        }

        if (contentType.startsWith("image/")) {
            return "image";
        } else if (contentType.equals("application/pdf")) {
            return "raw"; // PDFs use 'raw' resource type
        }

        return "raw"; // Default to raw for other types
    }

    /**
     * Build folder path: base/yyyy/MM/labOrderNumber/
     */
    private String buildFolderPath(String labOrderNumber) {
        LocalDate now = LocalDate.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));

        return String.format("%s/%s/%s/%s", baseFolder, year, month, labOrderNumber);
    }

    /**
     * Build public ID: lab_{labOrderNumber}_{timestamp}
     */
    private String buildPublicId(String labOrderNumber) {
        long timestamp = System.currentTimeMillis();
        return String.format("lab_%s_%d", labOrderNumber, timestamp);
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    @Override
    public byte[] downloadReport(String publicId) {
        log.info("Downloading report from Cloudinary: {}", publicId);

        try {
            // Get the secure URL for the resource
            String secureUrl = cloudinary.url()
                    .type("upload")
                    .resourceType("raw")
                    .secure(true)
                    .generate(publicId);

            log.debug("Generated secure URL for download: {}", secureUrl);

            // Download file from the secure URL
            java.net.URL url = new java.net.URL(secureUrl);
            java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);

            try (java.io.InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] fileContent = outputStream.toByteArray();
                log.info("✓ Downloaded report successfully: {} bytes", fileContent.length);

                return fileContent;

            }

        } catch (IOException e) {
            log.error("Failed to download report from Cloudinary: {}", publicId, e);
            throw new CloudStorageException("Failed to download report: " + e.getMessage(), e);
        }
    }
}
