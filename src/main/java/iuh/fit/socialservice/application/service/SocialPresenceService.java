package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.response.PresenceStatusCommand;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SocialPresenceService {
    void heartbeat(UUID userId, String sessionId);
    void disconnect(String sessionId, UUID fallbackUserId);
    List<PresenceStatusCommand> getPresence(Collection<UUID> userIds);
}
