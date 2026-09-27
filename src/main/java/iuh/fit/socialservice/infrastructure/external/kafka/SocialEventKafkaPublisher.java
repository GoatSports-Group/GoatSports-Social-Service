package iuh.fit.socialservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "social.events", name = "producer-enabled", havingValue = "true")
public class SocialEventKafkaPublisher implements SocialEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public SocialEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${social.events.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(String eventType, UUID receiverId, UUID actorId, UUID aggregateId,
                        Map<String, String> payload) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("receiverId", receiverId.toString());
            payload.forEach(body::put);

            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("eventId", UUID.randomUUID().toString());
            envelope.put("eventType", eventType);
            envelope.put("aggregateId", aggregateId.toString());
            envelope.put("producer", "social-service");
            envelope.put("occurredAt", Instant.now().toString());
            envelope.put("correlationId", String.valueOf(MDC.get("correlationId")));
            envelope.put("actorId", actorId.toString());
            envelope.set("payload", body);

            // JsonSerializer can nhan chinh object JSON; gui chuoi se bi serialize hai lan.
            kafkaTemplate.send(topic, aggregateId.toString(), envelope)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Không phát được sự kiện {}: {}", eventType, error.getMessage());
                        }
                    });
        } catch (Exception exception) {
            log.warn("Không phát được sự kiện {}: {}", eventType, exception.getMessage());
        }
    }
}
