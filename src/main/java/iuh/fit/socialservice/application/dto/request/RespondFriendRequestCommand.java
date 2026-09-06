package iuh.fit.socialservice.application.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondFriendRequestCommand {
    private UUID friendshipId;
    private UUID actorUserId;
    private boolean accepted;
}
