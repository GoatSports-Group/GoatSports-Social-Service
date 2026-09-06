package iuh.fit.socialservice.infrastructure.persistence.migration;

import com.mongodb.client.MongoCollection;
import iuh.fit.socialservice.domain.enums.MessageStatus;
import iuh.fit.socialservice.domain.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class LegacyMessageMigration implements ApplicationRunner {
    private static final String MESSAGES_COLLECTION = "messages";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        MongoCollection<Document> messages = mongoTemplate.getCollection(MESSAGES_COLLECTION);

        long normalizedTypes = messages.updateMany(
                in("type", "MATCH_INVITE", "LOCATION"),
                set("type", MessageType.SYSTEM.name())
        ).getModifiedCount();

        int migratedReceipts = 0;
        for (Document message : messages.find(exists("readByUserIds", true))) {
            List<Document> receipts = mergeReadReceipts(message);
            messages.updateOne(
                    new Document("_id", message.get("_id")),
                    new Document("$set", new Document("receipts", receipts))
                            .append("$unset", new Document("readByUserIds", ""))
            );
            migratedReceipts++;
        }

        if (normalizedTypes > 0 || migratedReceipts > 0) {
            log.info(
                    "Normalized {} legacy message types and migrated read receipts for {} messages",
                    normalizedTypes,
                    migratedReceipts
            );
        }
    }

    private List<Document> mergeReadReceipts(Document message) {
        Map<String, Document> receiptsByUserId = new LinkedHashMap<>();
        List<Document> existingReceipts = message.getList("receipts", Document.class, List.of());
        existingReceipts.forEach(receipt -> {
            String userId = receipt.getString("userId");
            if (userId != null) receiptsByUserId.put(userId, receipt);
        });

        Date occurredAt = toMongoDate(message.get("sentAt"));
        for (Object value : message.getList("readByUserIds", Object.class, List.of())) {
            String userId = String.valueOf(value);
            receiptsByUserId.put(userId, new Document()
                    .append("receiptId", UUID.randomUUID().toString())
                    .append("userId", userId)
                    .append("status", MessageStatus.READ.name())
                    .append("occurredAt", occurredAt));
        }
        return new ArrayList<>(receiptsByUserId.values());
    }

    private Date toMongoDate(Object value) {
        if (value instanceof Date date) return date;
        if (value instanceof LocalDateTime dateTime) {
            return Date.from(dateTime.toInstant(ZoneOffset.UTC));
        }
        return new Date();
    }
}
