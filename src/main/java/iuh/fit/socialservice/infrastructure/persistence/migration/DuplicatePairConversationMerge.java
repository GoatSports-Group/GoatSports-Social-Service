package iuh.fit.socialservice.infrastructure.persistence.migration;

import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.enums.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Trước đây mỗi phiên ghép kèo tạo một hội thoại MATCH mới, nên hai người chơi ghép
 * nhiều kèo sẽ có nhiều luồng chat trùng nhau trong danh sách tin nhắn. Từ nay
 * {@code SocialChatServiceImpl#createGroupConversation} dùng lại luồng sẵn có; runner
 * này gộp phần dữ liệu trùng đã sinh ra trước đó về đúng một luồng cho mỗi cặp.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DuplicatePairConversationMerge implements ApplicationRunner {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, List<Conversation>> byPair = conversationRepository.findAllPairConversations().stream()
                .filter(conversation -> pairKey(conversation) != null)
                .collect(Collectors.groupingBy(
                        DuplicatePairConversationMerge::pairKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int mergedPairs = 0;
        int removedConversations = 0;
        for (List<Conversation> duplicates : byPair.values()) {
            if (duplicates.size() < 2) continue;
            Conversation keeper = chooseKeeper(duplicates);
            long movedMessages = 0;
            for (Conversation duplicate : duplicates) {
                if (duplicate.getConversationId().equals(keeper.getConversationId())) continue;
                movedMessages += messageRepository.reassignConversation(
                        duplicate.getConversationId(),
                        keeper.getConversationId()
                );
                carryContext(keeper, duplicate);
                conversationRepository.deleteById(duplicate.getConversationId());
                removedConversations++;
            }
            refreshLastMessage(keeper);
            conversationRepository.save(keeper);
            mergedPairs++;
            log.info(
                    "Merged {} duplicate conversations into {} ({} messages moved)",
                    duplicates.size() - 1,
                    keeper.getConversationId(),
                    movedMessages
            );
        }

        if (removedConversations > 0) {
            log.info(
                    "Duplicate conversation cleanup finished: {} pairs merged, {} conversations removed",
                    mergedPairs,
                    removedConversations
            );
        }
    }

    /**
     * Ưu tiên giữ luồng chat cá nhân (DIRECT) để không đổi tên hội thoại riêng của
     * người dùng thành "Kèo ..."; nếu toàn bộ đều là MATCH thì giữ luồng cũ nhất.
     */
    private static Conversation chooseKeeper(List<Conversation> duplicates) {
        return duplicates.stream()
                .min(Comparator
                        .comparing((Conversation item) -> item.getType() == ConversationType.DIRECT ? 0 : 1)
                        .thenComparing(item -> item.getCreatedAt() == null ? LocalDateTime.MAX : item.getCreatedAt()))
                .orElseThrow();
    }

    /** Giữ lại kèo gần nhất mà cặp này đã chơi cùng nhau. */
    private void carryContext(Conversation keeper, Conversation duplicate) {
        if (duplicate.getContextId() == null) return;
        LocalDateTime keeperActivity = activityAt(keeper);
        LocalDateTime duplicateActivity = activityAt(duplicate);
        if (keeper.getContextId() == null || duplicateActivity.isAfter(keeperActivity)) {
            keeper.setContextId(duplicate.getContextId());
        }
    }

    private void refreshLastMessage(Conversation keeper) {
        Message latest = messageRepository.findLatestByConversationId(keeper.getConversationId()).orElse(null);
        if (latest == null) return;
        keeper.setLastMessageId(latest.getMessageId());
        keeper.setLastMessageAt(latest.getSentAt());
        keeper.setUpdatedAt(LocalDateTime.now());
    }

    private static LocalDateTime activityAt(Conversation conversation) {
        if (conversation.getLastMessageAt() != null) return conversation.getLastMessageAt();
        if (conversation.getUpdatedAt() != null) return conversation.getUpdatedAt();
        return conversation.getCreatedAt() == null ? LocalDateTime.MIN : conversation.getCreatedAt();
    }

    /** Khóa nhận diện một cặp người chơi, không phụ thuộc thứ tự thành viên. */
    private static String pairKey(Conversation conversation) {
        List<ConversationMember> members = conversation.getMembers();
        if (members == null) return null;
        List<String> userIds = members.stream()
                .filter(member -> member.getLeftAt() == null)
                .map(ConversationMember::getUserId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .distinct()
                .sorted()
                .toList();
        return userIds.size() == 2 ? String.join("|", userIds) : null;
    }
}
