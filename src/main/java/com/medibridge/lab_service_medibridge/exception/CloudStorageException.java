package com.medibridge.lab_service_medibridge.exception;

/**
 * Exception thrown when cloud storage operations fail
 */
public class CloudStorageException extends RuntimeException {

    public CloudStorageException(String message) {
        super(message);
    }

    public CloudStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
