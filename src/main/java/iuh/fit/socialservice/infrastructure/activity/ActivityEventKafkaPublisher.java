package iuh.fit.socialservice.infrastructure.activity;

import iuh.fit.socialservice.application.port.external.ActivityEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "activity.events.enabled", havingValue = "true")
public class ActivityEventKafkaPublisher implements ActivityEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    @SuppressWarnings("unchecked")
    public ActivityEventKafkaPublisher(ProducerFactory<?, ?> producerFactory,
                                       @Value("${activity.events.topic}") String topic) {
        // Kafka chết thì send() chờ metadata tới 60s mặc định, ngay trên luồng xử lý request (bấm thích).
        this.kafkaTemplate = new KafkaTemplate<>((ProducerFactory<String, Object>) producerFactory,
                Map.of(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2_000));
        this.topic = topic;
    }

    @Override
    public void publish(String eventType, UUID userId, Map<String, Object> payload) {
        if (userId == null) return;
        try {
            send(eventType, userId, payload, Instant.now()).whenComplete((result, error) -> {
                if (error != null) log.warn("Không phát được hành vi {}: {}", eventType, error.getMessage());
            });
        } catch (RuntimeException exception) {
            log.warn("Không phát được hành vi {}: {}", eventType, exception.getMessage());
        }
    }

    /** Gửi và trả kết quả, cho job nạp bù cần biết đã gửi được hết chưa và giữ đúng thời điểm gốc. */
    public CompletableFuture<SendResult<String, Object>> send(String eventType, UUID userId,
                                                              Map<String, Object> payload, Instant occurredAt) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("producer", "social-service");
        envelope.put("occurredAt", occurredAt.toString());
        envelope.put("userId", userId.toString());
        envelope.put("payload", payload);
        // Khóa = người dùng: thích rồi bỏ thích của một người vào cùng partition nên giữ đúng thứ tự.
        return kafkaTemplate.send(topic, userId.toString(), envelope);
    }
}
