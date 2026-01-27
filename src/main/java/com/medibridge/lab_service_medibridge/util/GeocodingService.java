package com.medibridge.lab_service_medibridge.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.medibridge.lab_service_medibridge.domain.LabOrder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service to simulate geocoding based on address string.
 * Uses deterministic hashing to ensure the same address always yields the same
 * "random" coordinates,
 * centered around a base location (Hospital).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final com.medibridge.lab_service_medibridge.client.rest.UserServiceClient userServiceClient;

    // Defaults (Paris hospital) - acting as fallback
    private double baseLat = 48.813893;
    private double baseLng = 2.365315;

    // Spread radius (approx 0.05 degrees ~= 5km)
    private static final double SPREAD = 0.05;

    @PostConstruct
    public void init() {
        refreshBaseLocation();
    }

    // Refresh every 30 mins or on demand
    @Scheduled(fixedRate = 1800000)
    public void refreshBaseLocation() {
        try {
            JsonNode root = userServiceClient.getOrganizationAddress();
            if (root != null && root.has("data")) {
                JsonNode data = root.get("data");
                if (data.has("latitude") && data.has("longitude")) {
                    this.baseLat = data.get("latitude").asDouble();
                    this.baseLng = data.get("longitude").asDouble();
                    log.info("Refreshed base hospital coordinates to: {}, {}", baseLat, baseLng);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch base hospital address from User Service. Using default/cached: {}, {}", baseLat,
                    baseLng);
        }
    }

    public static class Coordinates {
        public double lat;
        public double lng;

        public Coordinates(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    public Coordinates resolveCoordinates(LabOrder order) {
        // If we had stored coordinates, return them (not present in LabOrder entity
        // currently)
        // So we fallback to address hashing.

        String addressString = (order.getAddressLine1() + order.getCity() + order.getPostalCode()).trim();

        if (addressString.isEmpty()) {
            return new Coordinates(baseLat, baseLng);
        }

        try {
            // Deterministic "Random" based on address hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(addressString.getBytes(StandardCharsets.UTF_8));

            // Use bytes to generate offset
            long val = 0;
            for (int i = 0; i < 8; i++) {
                val = (val << 8) + (hash[i] & 0xff);
            }

            // Normalize to -1.0 to 1.0
            double normalized = (val % 100000) / 100000.0; // 0 to 1
            double latOffset = (normalized - 0.5) * SPREAD * 2; // -Spread to +Spread

            // Generate another for longitude using next bytes
            long val2 = 0;
            for (int i = 8; i < 16; i++) {
                val2 = (val2 << 8) + (hash[i] & 0xff);
            }
            double normalized2 = (val2 % 100000) / 100000.0;
            double lngOffset = (normalized2 - 0.5) * SPREAD * 2;

            return new Coordinates(baseLat + latOffset, baseLng + lngOffset);

        } catch (NoSuchAlgorithmException e) {
            return new Coordinates(baseLat, baseLng);
        }
    }
}
