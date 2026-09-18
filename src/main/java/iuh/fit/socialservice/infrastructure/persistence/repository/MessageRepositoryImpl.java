package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.infrastructure.persistence.entity.MessageEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.MessageReceiptDoc;
import iuh.fit.socialservice.infrastructure.persistence.mapper.MessageEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoMessageRepository;
import iuh.fit.socialservice.domain.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {
    private final MongoMessageRepository mongoMessageRepository;
    private final MessageEntityMapper messageEntityMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Message save(Message message) {
        MessageEntity entity = messageEntityMapper.toEntity(message);
        if (entity.getMessageId() == null) {
            entity.setMessageId(UUID.randomUUID().toString());
        }
        MessageEntity saved = mongoMessageRepository.save(entity);
        return messageEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Message> findById(UUID messageId) {
        return mongoMessageRepository.findById(messageId.toString()).map(messageEntityMapper::toDomain);
    }

    @Override
    public Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) return Optional.empty();
        return mongoMessageRepository.findBySenderIdAndClientMessageId(senderId.toString(), clientMessageId)
                .map(messageEntityMapper::toDomain);
    }

    @Override
    public Page<Message> findByConversationId(UUID conversationId, Pageable pageable) {
        return mongoMessageRepository.findByConversationIdOrderBySentAtDesc(conversationId.toString(), pageable)
                .map(messageEntityMapper::toDomain);
    }

    @Override
    public Optional<Message> findLatestByConversationId(UUID conversationId) {
        return mongoMessageRepository.findFirstByConversationIdOrderBySentAtDesc(conversationId.toString())
                .map(messageEntityMapper::toDomain);
    }

    @Override
    public void markConversationAsRead(UUID conversationId, UUID userId) {
        String userIdValue = userId.toString();
        List<MessageEntity> changed = new ArrayList<>();
        for (MessageEntity message : mongoMessageRepository.findByConversationIdOrderBySentAtAsc(conversationId.toString())) {
            if (userIdValue.equals(message.getSenderId())) continue;
            List<MessageReceiptDoc> receipts = message.getReceipts() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(message.getReceipts());
            MessageReceiptDoc receipt = receipts.stream()
                    .filter(item -> userIdValue.equals(item.getUserId()))
                    .findFirst()
                    .orElse(null);
            if (receipt == null) {
                receipts.add(MessageReceiptDoc.builder()
                        .receiptId(UUID.randomUUID().toString())
                        .userId(userIdValue)
                        .status(MessageStatus.READ)
                        .occurredAt(LocalDateTime.now())
                        .build());
            } else {
                receipt.setStatus(MessageStatus.READ);
                receipt.setOccurredAt(LocalDateTime.now());
            }
            message.setReceipts(receipts);
            message.setStatus(MessageStatus.READ);
            changed.add(message);
        }
        if (!changed.isEmpty()) mongoMessageRepository.saveAll(changed);
    }

    @Override
    public long countUnreadMessages(UUID conversationId, UUID userId) {
        Criteria readReceipt = Criteria.where("receipts").elemMatch(
                Criteria.where("userId").is(userId.toString()).and("status").is(MessageStatus.READ)
        );
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("conversationId").is(conversationId.toString()),
                Criteria.where("senderId").ne(userId.toString()),
                new Criteria().norOperator(readReceipt)
        ));
        return mongoTemplate.count(query, MessageEntity.class);
    }

    @Override
    public void deleteById(UUID messageId) {
        mongoMessageRepository.deleteById(messageId.toString());
    }

    @Override
    public long reassignConversation(UUID fromConversationId, UUID toConversationId) {
        if (fromConversationId == null || toConversationId == null || fromConversationId.equals(toConversationId)) {
            return 0;
        }
        return mongoTemplate.updateMulti(
                Query.query(Criteria.where("conversationId").is(fromConversationId.toString())),
                new Update().set("conversationId", toConversationId.toString()),
                MessageEntity.class
        ).getModifiedCount();
    }
}
