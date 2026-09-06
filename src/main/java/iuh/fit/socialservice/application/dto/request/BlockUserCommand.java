package iuh.fit.socialservice.application.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserCommand {
    private UUID blockerId;
    private UUID blockedUserId;
    private String reason;
}
