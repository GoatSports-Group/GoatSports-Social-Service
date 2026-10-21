package iuh.fit.socialservice.infrastructure.accountdeletion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** auth-service vừa ẩn danh hóa một tài khoản (hết 30 ngày chờ xóa): xóa nội dung cộng đồng của người đó. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-deleted.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class UserDeletedConsumer {
    private final ObjectMapper objectMapper;
    private final UserContentEraser eraser;

    @KafkaListener(topics = "${user-deleted.topic:user-deleted-topic}", groupId = "social-service-user-deleted",
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void consume(String message) throws Exception {
        eraser.erase(UUID.fromString(objectMapper.readTree(message).path("userId").asText()));
    }
}
