package iuh.fit.socialservice.presentation.response;

import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private UUID friendshipId;
    private UUID requesterId;
    private UUID addresseeId;
    private FriendshipStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
