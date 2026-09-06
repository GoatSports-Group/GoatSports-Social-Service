package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.ConversationType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {
    private UUID conversationId;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private LocalDateTime lastMessageAt;
    private UUID lastMessageId;
    private UUID contextId;
    private List<ConversationMember> members;
}
