package iuh.fit.socialservice.application.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockResponseCommand {
    private UUID blockId;
    private UUID blockerId;
    private UUID blockedUserId;
    private String reason;
    private LocalDateTime blockedAt;
}
