package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.LocationPing;
import com.medibridge.lab_service_medibridge.domain.OrganizationSettings;
import com.medibridge.lab_service_medibridge.domain.enums.CollectionTaskStatus;
import com.medibridge.lab_service_medibridge.domain.model.LocationUpdateRequest;
import com.medibridge.lab_service_medibridge.domain.model.TrackingStateResponse;
import com.medibridge.lab_service_medibridge.domain.repository.LocationPingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTrackingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LocationPingRepository locationPingRepository; // Optional async persistence
    private final TrackingBroadcaster trackingBroadcaster;
    private final com.medibridge.lab_service_medibridge.repository.LabOrderRepository labOrderRepository;
    private final com.medibridge.lab_service_medibridge.repository.OrganizationSettingsRepository organizationSettingsRepository;

    private static final String KEY_PREFIX_LIVE_LOC = "live:task:";
    private static final String KEY_GEO_PHLEB = "geo:phleb:available";
    // TTL for live keys (Auto-expire after 24h to prevent stale data buildup)
    private static final long LIVE_KEY_TTL_SECONDS = 86400;

    /**
     * Update live location for a task
     * 
     * @param labOrderId Needed for broadcasting topic
     */
    public void updateLocation(UUID taskId, UUID labOrderId, String phlebotomistId, LocationUpdateRequest update) {
        String key = KEY_PREFIX_LIVE_LOC + taskId;

        // 1. Check for stale ping (Optimistic via timestamp in hash if present, or just
        // simple overwrite for MVP)
        // For production: HGET timestamp and compare.
        // Here we overwrite for speed, assuming client sends ordered updates mostly.

        try {
            // Write Hash to Redis
            Map<String, Object> hash = Map.of(
                    "lat", update.getLatitude(),
                    "lng", update.getLongitude(),
                    "ts", update.getTimestamp().toEpochSecond(ZoneOffset.UTC),
                    "accuracy", update.getAccuracy() != null ? update.getAccuracy() : 0.0,
                    "bearing", update.getBearing() != null ? update.getBearing() : 0.0);

            redisTemplate.opsForHash().putAll(key, hash);
            redisTemplate.expire(key, java.time.Duration.ofSeconds(LIVE_KEY_TTL_SECONDS));

            // 2. Update GEO index (for "nearest phleb" queries)
            // Member is phlebotomistId:taskId to allow one phleb to have multiple active
            // tasks?
            // Usually phleb has one location. Let's index by phlebId.
            redisTemplate.opsForGeo().add(KEY_GEO_PHLEB,
                    new Point(update.getLongitude(), update.getLatitude()),
                    phlebotomistId);

            // 3. Get order and organization details for complete tracking info
            LabOrder order = labOrderRepository.findById(labOrderId).orElse(null);
            OrganizationSettings orgSettings = organizationSettingsRepository.findFirstByOrderByIdAsc().orElse(null);

            // Build destination location
            TrackingStateResponse.LocationMinimal destinationLocation = null;
            if (order != null && order.getDeliveryLat() != null && order.getDeliveryLng() != null) {
                destinationLocation = new TrackingStateResponse.LocationMinimal(
                    order.getDeliveryLat().doubleValue(),
                    order.getDeliveryLng().doubleValue(),
                    LocalDateTime.now()
                );
            }

            // Build hospital location
            TrackingStateResponse.LocationMinimal hospitalLocation = null;
            if (orgSettings != null && orgSettings.getHospitalLat() != null && orgSettings.getHospitalLng() != null) {
                hospitalLocation = new TrackingStateResponse.LocationMinimal(
                    orgSettings.getHospitalLat().doubleValue(),
                    orgSettings.getHospitalLng().doubleValue(),
                    LocalDateTime.now()
                );
            }

            // 4. Broadcast WebSocket Event with full context
            TrackingStateResponse response = TrackingStateResponse.builder()
                    .labOrderId(labOrderId)
                    .taskId(taskId)
                    .status(CollectionTaskStatus.EN_ROUTE) // Assuming EN_ROUTE if getting pings? Or pass actual status.
                    .lastUpdatedAt(update.getTimestamp())
                    .lastLocation(new TrackingStateResponse.LocationMinimal(update.getLatitude(), update.getLongitude(),
                            update.getTimestamp()))
                    .patientLocation(destinationLocation)
                    .hospitalLocation(hospitalLocation)
                    .estimatedArrivalMinutes(calculateEta(update)) // Simple heuristic
                    .build();

            trackingBroadcaster.broadcastTrackingUpdate(labOrderId, response);

            // 5. Async Persist (Sampling / Batching recommended for high volume)
            // Persist every ping for audit trail? Or sample every minute?
            // For now, persist all async.
            CompletableFuture.runAsync(() -> persistLocationPing(taskId, phlebotomistId, update));

        } catch (Exception e) {
            log.error("Failed to update tracking for task {}", taskId, e);
        }
    }

    public TrackingStateResponse.LocationMinimal getLatestLocation(UUID taskId) {
        String key = KEY_PREFIX_LIVE_LOC + taskId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty())
            return null;

        try {
            Double lat = (Double) entries.get("lat");
            Double lng = (Double) entries.get("lng");
            Long ts = ((Number) entries.get("ts")).longValue();

            return new TrackingStateResponse.LocationMinimal(lat, lng,
                    LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC));
        } catch (Exception e) {
            log.warn("Invalid location data in Redis for task {}", taskId);
            return null;
        }
    }

    private void persistLocationPing(UUID taskId, String phlebId, LocationUpdateRequest update) {
        try {
            LocationPing ping = LocationPing.builder()
                    .taskId(taskId)
                    .phlebotomistId(phlebId)
                    .latitude(update.getLatitude())
                    .longitude(update.getLongitude())
                    .accuracy(update.getAccuracy())
                    .speed(update.getSpeed())
                    .bearing(update.getBearing())
                    .capturedAt(update.getTimestamp())
                    .build();
            locationPingRepository.save(ping);
        } catch (Exception e) {
            log.warn("Failed to persist location ping audit", e);
        }
    }

    private Integer calculateEta(LocationUpdateRequest loc) {
        // Placeholder: integration with Google Maps Distance Matrix API would go here
        // Return null or pseudo-value
        return 15; // Mock 15 mins
    }
}
