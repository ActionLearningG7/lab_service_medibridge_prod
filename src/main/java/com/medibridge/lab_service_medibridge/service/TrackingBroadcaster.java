package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.model.TrackingStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast live tracking update to subscribed patients/doctors
     */
    public void broadcastTrackingUpdate(UUID labOrderId, TrackingStateResponse payload) {
        if (labOrderId == null || payload == null)
            return;

        String destination = "/topic/lab/orders/" + labOrderId + "/tracking";
        messagingTemplate.convertAndSend(destination, payload);

        log.trace("Broadcasted tracking update for order {} to {}", labOrderId, destination);
    }
}
