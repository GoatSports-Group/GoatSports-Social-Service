package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship extends BaseEntity {
    private UUID friendshipId;
    private UUID requesterId;
    private UUID addresseeId;
    private FriendshipStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
