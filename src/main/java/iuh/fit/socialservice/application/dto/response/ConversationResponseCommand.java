package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.enums.MemberRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseCommand {
    private UUID conversationId;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID contextId;
    private UUID lastMessageId;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private UUID lastSenderId;
    private Long unreadCount;
    /** Rieng nguoi xem: da tat thong bao doan chat nay. */
    private Boolean muted;
    /** Rieng nguoi xem, chat hai nguoi: BLOCKED_BY_ME / BLOCKED_BY_THEM, null neu khong chan. */
    private String blockState;
    private List<MemberItemResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberItemResponse {
        private UUID userId;
        private MemberRole role;
        private LocalDateTime joinedAt;
        private UUID lastReadMessageId;
        private LocalDateTime leftAt;
    }
}
