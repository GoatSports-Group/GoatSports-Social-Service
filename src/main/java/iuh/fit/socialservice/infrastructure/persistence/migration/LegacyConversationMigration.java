package iuh.fit.socialservice.infrastructure.persistence.migration;

import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LegacyConversationMigration implements ApplicationRunner {
    private final LegacyConversationMongoRepository legacyRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = 0;
        for (LegacyConversationDocument legacy : legacyRepository.findAll()) {
            UUID conversationId = parseUuid(legacy.getConversationId());
            if (conversationId == null || conversationRepository.findById(conversationId).isPresent()) continue;

            List<ConversationMember> members = distinctMembers(conversationId, legacy.getMembers());
            if (members.size() < 2) {
                log.warn("Skipping legacy conversation {} because it has fewer than two valid members", conversationId);
                continue;
            }

            Message latestMessage = messageRepository.findLatestByConversationId(conversationId).orElse(null);
            Conversation conversation = Conversation.builder()
                    .conversationId(conversationId)
                    .type(toConversationType(legacy.getType()))
                    .name(legacy.getName())
                    .avatarUrl(legacy.getAvatarUrl())
                    .lastMessageId(latestMessage == null ? null : latestMessage.getMessageId())
                    .lastMessageAt(latestMessage == null ? legacy.getLastMessageAt() : latestMessage.getSentAt())
                    .members(members)
                    .createdAt(valueOrNow(legacy.getCreatedAt()))
                    .updatedAt(valueOrNow(legacy.getUpdatedAt()))
                    .build();
            conversationRepository.save(conversation);
            migrated++;
        }
        if (migrated > 0) log.info("Migrated {} legacy MongoDB conversations to PostgreSQL", migrated);
    }

    private List<ConversationMember> distinctMembers(
            UUID conversationId,
            List<LegacyConversationDocument.LegacyConversationMember> legacyMembers
    ) {
        if (legacyMembers == null) return List.of();

        Map<UUID, LegacyConversationDocument.LegacyConversationMember> distinctByUserId = new LinkedHashMap<>();
        legacyMembers.forEach(member -> {
            UUID userId = parseUuid(member.getUserId());
            if (userId != null) distinctByUserId.putIfAbsent(userId, member);
        });
        return distinctByUserId.values().stream()
                .map(member -> toMember(conversationId, member))
                .toList();
    }

    private ConversationMember toMember(
            UUID conversationId,
            LegacyConversationDocument.LegacyConversationMember legacy
    ) {
        LocalDateTime joinedAt = valueOrNow(legacy.getJoinedAt());
        return ConversationMember.builder()
                .memberId(valueOrRandom(parseUuid(legacy.getMemberId())))
                .conversationId(conversationId)
                .userId(parseUuid(legacy.getUserId()))
                .role(toMemberRole(legacy.getRole()))
                .joinedAt(joinedAt)
                .createdAt(joinedAt)
                .updatedAt(joinedAt)
                .build();
    }

    private ConversationType toConversationType(String value) {
        if ("MATCHMAKING".equalsIgnoreCase(value)) return ConversationType.MATCH;
        try {
            return ConversationType.valueOf(value == null ? "GROUP" : value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ConversationType.GROUP;
        }
    }

    private MemberRole toMemberRole(String value) {
        try {
            return MemberRole.valueOf(value == null ? "MEMBER" : value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return MemberRole.MEMBER;
        }
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID valueOrRandom(UUID value) {
        return value == null ? UUID.randomUUID() : value;
    }

    private LocalDateTime valueOrNow(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }
}
