package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.MemberRole;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMember extends BaseEntity {
    private UUID memberId;
    private UUID conversationId;
    private UUID userId;
    private MemberRole role;
    private LocalDateTime joinedAt;
    private UUID lastReadMessageId;
    private LocalDateTime leftAt;
    private LocalDateTime clearedAt;
    private boolean muted;
}
