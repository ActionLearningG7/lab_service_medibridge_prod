package com.medibridge.lab_service_medibridge.exception;

/**
 * Exception thrown when uploaded file is invalid
 * (wrong type, too large, corrupted, etc.)
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
