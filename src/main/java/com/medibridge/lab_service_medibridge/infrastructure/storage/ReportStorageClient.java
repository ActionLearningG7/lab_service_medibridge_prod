package com.medibridge.lab_service_medibridge.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for report storage operations
 * Allows switching between cloud providers (Cloudinary, AWS S3, etc.)
 */
public interface ReportStorageClient {

    /**
     * Upload a lab report file to cloud storage
     *
     * @param file MultipartFile from upload request
     * @param labOrderNumber Lab order identifier for folder structure
     * @param patientId Patient identifier for access control
     * @return ReportUploadResult containing secure URL and metadata
     * @throws InvalidFileException if file type/size invalid
     * @throws CloudStorageException if upload fails
     */
    ReportUploadResult uploadReport(MultipartFile file, String labOrderNumber, String patientId);

    /**
     * Delete a report from cloud storage
     *
     * @param publicId Cloudinary public ID
     * @return true if deleted successfully
     * @throws CloudStorageException if deletion fails
     */
    boolean deleteReport(String publicId);

    /**
     * Validate file before upload
     *
     * @param file MultipartFile to validate
     * @throws InvalidFileException if validation fails
     */
    void validateFile(MultipartFile file);

    /**
     * Download a report file from cloud storage
     *
     * @param publicId Cloudinary public ID of the file
     * @return byte array containing the file content
     * @throws CloudStorageException if download fails
     */
    byte[] downloadReport(String publicId);
}
