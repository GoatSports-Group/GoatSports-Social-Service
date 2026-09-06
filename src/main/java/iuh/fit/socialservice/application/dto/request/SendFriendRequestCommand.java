package iuh.fit.socialservice.application.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestCommand {
    private UUID requesterId;
    private UUID addresseeId;
}
