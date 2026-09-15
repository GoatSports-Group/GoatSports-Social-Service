package iuh.fit.socialservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceStatusCommand {
    private UUID userId;
    private boolean online;
    private Instant lastSeenAt;
}
