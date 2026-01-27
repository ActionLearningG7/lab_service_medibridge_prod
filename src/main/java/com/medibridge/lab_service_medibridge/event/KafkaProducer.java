package com.medibridge.lab_service_medibridge.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object event) {
        try {
            log.info("Publishing event to topic {}: {}", topic, event);
            kafkaTemplate.send(topic, event);
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}", topic, e);
            // Outbox pattern should be implemented here or via Debezium/Polling in a real
            // enterprise setup.
            // For this scope, we assume immediate send, or wrap in transaction listener.
        }
    }
}
