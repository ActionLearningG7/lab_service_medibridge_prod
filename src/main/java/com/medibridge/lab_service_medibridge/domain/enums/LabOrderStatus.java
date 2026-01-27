package com.medibridge.lab_service_medibridge.domain.enums;

public enum LabOrderStatus {
    CREATED,
    CONFIRMED,
    SCHEDULED,
    ASSIGNED, // Optional visibility
    IN_COLLECTION, // New
    COLLECTED, // added
    IN_TRANSIT,
    AT_LAB, // New
    TESTING, // New
    RESULT_UPLOADED, // Result file uploaded by phlebotomist admin
    REPORT_READY, // New
    REPORT_PUBLISHED, // New
    COMPLETED,
    CANCELLED
}
