package iuh.fit.socialservice.infrastructure.persistence.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.entity.MessageReceipt;
import iuh.fit.socialservice.domain.enums.MessageStatus;
import iuh.fit.socialservice.domain.enums.MessageType;
import iuh.fit.socialservice.infrastructure.persistence.mapper.MessageEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoMessageRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Cuon tin nhan hai chieu tren MongoDB that (bo qua khi khong co Mongo o {@code SOCIAL_IT_MONGO_URI}).
 * 10 tin cach nhau mot phut; nguoi xem da doc 6 tin dau, 4 tin cuoi chua doc.
 */
class MessageCursorMongoTest {
    private static MongoClient client;
    private static MongoTemplate template;
    private static String database;

    private final UUID conversation = UUID.randomUUID();
    private final UUID viewer = UUID.randomUUID();
    private final UUID friend = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS);
    private final List<Message> sent = new ArrayList<>();
    private MessageRepositoryImpl messages;

    @BeforeAll
    static void connect() {
        String uri = System.getenv().getOrDefault("SOCIAL_IT_MONGO_URI",
                "mongodb://localhost:27017/?serverSelectionTimeoutMS=1500&connectTimeoutMS=1500");
        try {
            client = MongoClients.create(uri);
            database = "goat_social_it_" + UUID.randomUUID().toString().substring(0, 8);
            client.getDatabase(database).runCommand(new Document("ping", 1));
            template = new MongoTemplate(client, database);
        } catch (RuntimeException unavailable) {
            assumeTrue(false, "MongoDB không sẵn sàng: " + unavailable.getMessage());
        }
    }

    @AfterAll
    static void dropDatabase() {
        if (client == null) return;
        if (template != null) client.getDatabase(database).drop();
        client.close();
    }

    @BeforeEach
    void seed() {
        template.getDb().drop();
        messages = new MessageRepositoryImpl(new MongoRepositoryFactory(template).getRepository(MongoMessageRepository.class),
                new MessageEntityMapper(), template);
        for (int index = 0; index < 10; index++) {
            boolean read = index < 6;
            sent.add(messages.save(Message.builder()
                    .conversationId(conversation).senderId(friend).clientMessageId("c" + index).content("tin " + index)
                    .type(MessageType.TEXT).status(MessageStatus.SENT).deleted(false).sentAt(start.plusMinutes(index))
                    .attachments(List.of())
                    .receipts(List.of(MessageReceipt.builder().receiptId(UUID.randomUUID()).userId(viewer)
                            .status(read ? MessageStatus.READ : MessageStatus.SENT).occurredAt(start).build()))
                    .build()));
        }
    }

    @Test
    void cursorsPageBothWaysWithoutGapsOrRepeats() {
        // Moi nhat truoc khi cuon len; cu nhat truoc khi cuon xuong.
        assertThat(contents(messages.findOlder(conversation, start.plusMinutes(5), null, 3)))
                .containsExactly("tin 4", "tin 3", "tin 2");
        assertThat(contents(messages.findNewer(conversation, start.plusMinutes(5), false, null, 3)))
                .containsExactly("tin 6", "tin 7", "tin 8");
        assertThat(contents(messages.findNewer(conversation, start.plusMinutes(5), true, null, 2)))
                .containsExactly("tin 5", "tin 6");
    }

    @Test
    void theClearMarkIsAFloorInBothDirections() {
        LocalDateTime cleared = start.plusMinutes(7);
        assertThat(contents(messages.findOlder(conversation, start.plusMinutes(9), cleared, 10))).containsExactly("tin 8");
        assertThat(contents(messages.findNewer(conversation, start, true, cleared, 10))).containsExactly("tin 8", "tin 9");
    }

    @Test
    void firstUnreadIsTheOldestMessageWithoutTheViewersReadReceipt() {
        assertThat(messages.findFirstUnread(conversation, viewer, null)).map(Message::getContent).contains("tin 6");
        assertThat(messages.findFirstUnread(conversation, viewer, start.plusMinutes(8)))
                .map(Message::getContent).contains("tin 9");
        assertThat(messages.findFirstUnread(conversation, friend, null)).isEmpty();
    }

    private static List<String> contents(List<Message> list) {
        return list.stream().map(Message::getContent).toList();
    }
}
