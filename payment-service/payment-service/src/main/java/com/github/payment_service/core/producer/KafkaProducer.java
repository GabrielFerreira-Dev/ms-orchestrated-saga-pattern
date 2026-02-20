package com.github.payment_service.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestratorTopic;

    public void sendEvent(String payload) {
        try{
            log.info("Sending event to Kafka topic {}: {}", orchestratorTopic, payload);
            kafkaTemplate.send(orchestratorTopic, payload);
        } catch (Exception e) {
            log.error("Failed to send event to Kafka topic {}: {}", orchestratorTopic, e.getMessage());
        }
    }

}
