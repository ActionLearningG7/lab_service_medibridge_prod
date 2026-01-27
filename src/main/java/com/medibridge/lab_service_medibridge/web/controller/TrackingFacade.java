package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.CollectionTask;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.OrganizationSettings;
import com.medibridge.lab_service_medibridge.domain.model.CollectionTaskResponse;
import com.medibridge.lab_service_medibridge.domain.model.TrackingStateResponse;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.repository.LabOrderRepository;
import com.medibridge.lab_service_medibridge.repository.OrganizationSettingsRepository;
import com.medibridge.lab_service_medibridge.service.RedisTrackingService;
import com.medibridge.lab_service_medibridge.util.LabMapper;
import com.medibridge.lab_service_medibridge.web.dto.NavigationContextResponse;
import com.medibridge.lab_service_medibridge.web.dto.TrackingContextResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class TrackingFacade {
    private final LabOrderRepository labOrderRepository;
    private final CollectionTaskRepository collectionTaskRepository;
    private final RedisTrackingService redisTrackingService;
    private final LabMapper mapper;
    private final com.medibridge.lab_service_medibridge.util.GeocodingService geocodingService;
    private final OrganizationSettingsRepository organizationSettingsRepository;


    public ResponseEntity<TrackingStateResponse> getTrackingState(UUID orderId, String userId, boolean isDoctor) {
        LabOrder order = labOrderRepository.findById(orderId).orElse(null);
        if (order == null)
            return ResponseEntity.notFound().build();

        // Ownership Check
        boolean isOwner = isDoctor ? userId.equals(order.getDoctorId())
                : userId.equals(order.getPatientId());
        if (!isOwner)
            return ResponseEntity.status(403).build();

        CollectionTask task = collectionTaskRepository.findByLabOrderId(orderId).orElse(null);

        // Resolve Patient Location
        var coords = geocodingService.resolveCoordinates(order);
        TrackingStateResponse.LocationMinimal patientLoc = new TrackingStateResponse.LocationMinimal(
                coords.lat, coords.lng, LocalDateTime.now());

        if (task == null) {
            // Return empty tracking if no task (e.g., WALK_IN)
            return ResponseEntity.ok(TrackingStateResponse.builder()
                    .labOrderId(orderId)
                    .patientLocation(patientLoc)
                    .status(null)
                    .build());
        }

        TrackingStateResponse.LocationMinimal liveLoc = redisTrackingService.getLatestLocation(task.getId());

        TrackingStateResponse response = TrackingStateResponse.builder()
                .labOrderId(orderId)
                .taskId(task.getId())
                .status(task.getStatus())
                .lastLocation(liveLoc)
                .patientLocation(patientLoc)
                .lastUpdatedAt(liveLoc != null ? liveLoc.getCapturedAt() : LocalDateTime.now()) // heuristic
                // .estimatedArrivalMinutes(...) // could be added here
                .build();

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<CollectionTaskResponse> getTaskDetails(UUID orderId, String userId) {
        LabOrder order = labOrderRepository.findById(orderId).orElse(null);
        if (order == null)
            return ResponseEntity.notFound().build();

        if (!userId.equals(order.getPatientId()))
            return ResponseEntity.status(403).build();

        CollectionTask task = collectionTaskRepository.findByLabOrderId(orderId).orElse(null);
        if (task == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(mapper.toTaskResponse(task));
    }

    /**
     * Get tracking context for patient/doctor viewing live tracking
     */
    public ResponseEntity<TrackingContextResponse> getTrackingContext(UUID orderId, String userId, boolean isDoctor) {
        log.info("Getting tracking context for order: {}", orderId);

        LabOrder order = labOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        // Ownership Check
        boolean isOwner = isDoctor ? userId.equals(order.getDoctorId())
                : userId.equals(order.getPatientId());
        if (!isOwner) {
            log.warn("Access denied for user {} to order {}", userId, orderId);
            return ResponseEntity.status(403).build();
        }

        // Get hospital location from organization settings
        OrganizationSettings orgSettings = organizationSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        TrackingContextResponse.LocationDto hospitalLocation = null;

        if (orgSettings == null) {
            log.error("❌ No organization settings found in database! Please run insert_hospital_data.sql");
        } else {
            log.info("✅ Organization settings found: id={}, name={}", orgSettings.getId(), orgSettings.getOrganizationName());
            log.info("   Address: {}, {}, {}", orgSettings.getAddressLine1(), orgSettings.getCity(), orgSettings.getCountry());
            log.info("   Coordinates: lat={}, lng={}", orgSettings.getHospitalLat(), orgSettings.getHospitalLng());

            if (orgSettings.getHospitalLat() == null) {
                log.error("❌ hospital_lat is NULL in database! Data: {}", orgSettings);
            }
            if (orgSettings.getHospitalLng() == null) {
                log.error("❌ hospital_lng is NULL in database! Data: {}", orgSettings);
            }
        }

        if (orgSettings != null && orgSettings.getHospitalLat() != null && orgSettings.getHospitalLng() != null) {
            hospitalLocation = TrackingContextResponse.LocationDto.builder()
                    .lat(orgSettings.getHospitalLat().doubleValue())
                    .lng(orgSettings.getHospitalLng().doubleValue())
                    .address(buildHospitalAddress(orgSettings))
                    .build();
            log.info("✅ Hospital location configured: lat={}, lng={}, address='{}'",
                hospitalLocation.getLat(), hospitalLocation.getLng(), hospitalLocation.getAddress());
        } else {
            log.warn("❌ Hospital coordinates not configured in organization settings");
        }

        // Get destination (patient location)
        TrackingContextResponse.LocationDto destinationLocation = null;

        // Build full address string
        String fullAddress = buildFullAddress(order);
        log.info("Building destination for order {}: address='{}'", orderId, fullAddress);

        if (order.getDeliveryLat() != null && order.getDeliveryLng() != null) {
            destinationLocation = TrackingContextResponse.LocationDto.builder()
                    .lat(order.getDeliveryLat().doubleValue())
                    .lng(order.getDeliveryLng().doubleValue())
                    .address(fullAddress)
                    .build();
            log.info("Destination location: lat={}, lng={}, address='{}'",
                destinationLocation.getLat(), destinationLocation.getLng(), fullAddress);
        } else {
            log.warn("Order {} has no delivery coordinates. Address: '{}'", orderId, fullAddress);

            // Try to use geocoding service if available
            try {
                var coords = geocodingService.resolveCoordinates(order);
                if (coords != null) {
                    destinationLocation = TrackingContextResponse.LocationDto.builder()
                            .lat(coords.lat)
                            .lng(coords.lng)
                            .address(fullAddress)
                            .build();
                    log.info("Geocoded destination: lat={}, lng={}, address='{}'",
                        coords.lat, coords.lng, fullAddress);
                }
            } catch (Exception e) {
                log.error("Failed to geocode address for order {}", orderId, e);
            }
        }

        // Get collection task
        CollectionTask task = collectionTaskRepository.findByLabOrderId(orderId).orElse(null);

        TrackingContextResponse.PhlebotomistSummary phlebSummary = null;
        if (task != null && task.getPhlebotomistId() != null) {
            // TODO: Fetch phlebotomist details from user service via Feign
            phlebSummary = TrackingContextResponse.PhlebotomistSummary.builder()
                    .id(task.getPhlebotomistId())
                    .name("MediBridge Courier") // Placeholder
                    .phone("XXX-XXX-XXXX") // Placeholder
                    .build();
        }

        TrackingContextResponse response = TrackingContextResponse.builder()
                .hospitalLocation(hospitalLocation)
                .destinationLocation(destinationLocation)
                .status(order.getStatus().name())
                .phlebotomist(phlebSummary)
                .estimatedArrivalMinutes(15) // Placeholder - calculate from real-time data
                .build();

        log.info("Returning tracking context with hospital={}, destination={}",
            hospitalLocation != null, destinationLocation != null);

        return ResponseEntity.ok(response);
    }

    /**
     * Get navigation context for phlebotomist navigation view
     */
    public ResponseEntity<NavigationContextResponse> getNavigationContext(UUID taskId, String phlebId) {
        CollectionTask task = collectionTaskRepository.findById(taskId).orElse(null);
        if (task == null)
            return ResponseEntity.notFound().build();

        // Security check
        if (!phlebId.equals(task.getPhlebotomistId()))
            return ResponseEntity.status(403).build();

        LabOrder order = labOrderRepository.findById(task.getLabOrderId()).orElse(null);
        if (order == null)
            return ResponseEntity.notFound().build();

        // Get hospital location
        OrganizationSettings orgSettings = organizationSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        NavigationContextResponse.LocationDto hospitalLocation = null;

        if (orgSettings != null && orgSettings.getHospitalLat() != null && orgSettings.getHospitalLng() != null) {
            hospitalLocation = NavigationContextResponse.LocationDto.builder()
                    .lat(orgSettings.getHospitalLat().doubleValue())
                    .lng(orgSettings.getHospitalLng().doubleValue())
                    .address(buildHospitalAddress(orgSettings))
                    .build();
            log.info("Navigation - Hospital location: lat={}, lng={}, address='{}'",
                hospitalLocation.getLat(), hospitalLocation.getLng(), hospitalLocation.getAddress());
        } else {
            log.warn("Navigation - Hospital coordinates not available");
            if (orgSettings == null) {
                log.error("Navigation - No organization settings in database!");
            }
        }

        // Get destination (patient location)
        String fullAddress = buildFullAddress(order);
        NavigationContextResponse.LocationDto destinationLocation = null;
        if (order.getDeliveryLat() != null && order.getDeliveryLng() != null) {
            destinationLocation = NavigationContextResponse.LocationDto.builder()
                    .lat(order.getDeliveryLat().doubleValue())
                    .lng(order.getDeliveryLng().doubleValue())
                    .address(fullAddress)
                    .build();
            log.info("Navigation destination: lat={}, lng={}, address='{}'",
                destinationLocation.getLat(), destinationLocation.getLng(), fullAddress);
        } else {
            log.warn("Task {} order has no delivery coordinates. Address: '{}'", taskId, fullAddress);
        }

        NavigationContextResponse response = NavigationContextResponse.builder()
                .destination(destinationLocation)
                .hospital(hospitalLocation)
                .orderNumber(order.getOrderNumber())
                .patientName("Patient") // TODO: Fetch from user service
                .patientPhone(order.getContactPhone())
                .specialInstructions(order.getSpecialInstructions())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Build full address from order, handling null values
     */
    private String buildFullAddress(LabOrder order) {
        StringBuilder address = new StringBuilder();

        if (order.getAddressLine1() != null && !order.getAddressLine1().trim().isEmpty()) {
            address.append(order.getAddressLine1().trim());
        }

        if (order.getCity() != null && !order.getCity().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(order.getCity().trim());
        }

        if (order.getState() != null && !order.getState().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(order.getState().trim());
        }

        if (order.getPostalCode() != null && !order.getPostalCode().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(" ");
            }
            address.append(order.getPostalCode().trim());
        }

        if (order.getCountry() != null && !order.getCountry().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(order.getCountry().trim());
        }

        return !address.isEmpty() ? address.toString() : "Address not available";
    }

    /**
     * Build hospital address from organization settings
     */
    private String buildHospitalAddress(OrganizationSettings org) {
        StringBuilder address = new StringBuilder();

        if (org.getOrganizationName() != null && !org.getOrganizationName().trim().isEmpty()) {
            address.append(org.getOrganizationName().trim());
        }

        if (org.getAddressLine1() != null && !org.getAddressLine1().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(org.getAddressLine1().trim());
        }

        if (org.getCity() != null && !org.getCity().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(org.getCity().trim());
        }

        if (org.getPostalCode() != null && !org.getPostalCode().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(" ");
            }
            address.append(org.getPostalCode().trim());
        }

        if (org.getCountry() != null && !org.getCountry().trim().isEmpty()) {
            if (!address.isEmpty()) {
                address.append(", ");
            }
            address.append(org.getCountry().trim());
        }

        return !address.isEmpty() ? address.toString() : "Hospital";
    }
}


