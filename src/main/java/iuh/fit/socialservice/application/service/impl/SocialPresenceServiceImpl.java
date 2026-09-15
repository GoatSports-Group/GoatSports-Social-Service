package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.PresenceStatusCommand;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SocialPresenceServiceImpl implements SocialPresenceService {
    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(45);

    private final SocialWebSocketPublisher webSocketPublisher;
    private final Map<String, SessionPresence> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<UUID, PresenceStatusCommand> statusByUser = new ConcurrentHashMap<>();

    @Override
    public synchronized void heartbeat(UUID userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) return;

        Instant now = Instant.now();
        SessionPresence previousSession = sessions.put(sessionId, new SessionPresence(userId, now));
        if (previousSession != null && !previousSession.userId().equals(userId)) {
            removeSessionFromUser(previousSession.userId(), sessionId, now);
        }

        boolean wasOnline = isOnline(userId);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        PresenceStatusCommand onlineStatus = presence(userId, true, now);
        statusByUser.put(userId, onlineStatus);

        if (!wasOnline) webSocketPublisher.publishPresenceStatus(onlineStatus);
    }

    @Override
    public synchronized void disconnect(String sessionId, UUID fallbackUserId) {
        Instant now = Instant.now();
        SessionPresence session = sessionId == null ? null : sessions.remove(sessionId);
        UUID userId = session == null ? fallbackUserId : session.userId();
        if (userId == null) return;

        if (sessionId != null) {
            removeSessionFromUser(userId, sessionId, now);
            return;
        }

        markOfflineIfNeeded(userId, now);
    }

    @Override
    public List<PresenceStatusCommand> getPresence(Collection<UUID> userIds) {
        if (userIds == null) return List.of();
        return userIds.stream()
                .filter(userId -> userId != null)
                .distinct()
                .map(userId -> statusByUser.getOrDefault(userId, presence(userId, false, null)))
                .toList();
    }

    @Scheduled(fixedDelay = 15_000)
    public synchronized void expireStaleSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TIMEOUT);
        sessions.entrySet().stream()
                .filter(entry -> entry.getValue().lastHeartbeatAt().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(sessionId -> disconnect(sessionId, null));
    }

    private void removeSessionFromUser(UUID userId, String sessionId, Instant now) {
        Set<String> userSessions = sessionsByUser.get(userId);
        if (userSessions != null) {
            userSessions.remove(sessionId);
            if (userSessions.isEmpty()) sessionsByUser.remove(userId);
        }
        markOfflineIfNeeded(userId, now);
    }

    private void markOfflineIfNeeded(UUID userId, Instant now) {
        if (isOnline(userId)) return;
        PresenceStatusCommand previous = statusByUser.get(userId);
        PresenceStatusCommand offlineStatus = presence(userId, false, now);
        statusByUser.put(userId, offlineStatus);
        if (previous != null && previous.isOnline()) {
            webSocketPublisher.publishPresenceStatus(offlineStatus);
        }
    }

    private boolean isOnline(UUID userId) {
        Set<String> userSessions = sessionsByUser.get(userId);
        return userSessions != null && !userSessions.isEmpty();
    }

    private PresenceStatusCommand presence(UUID userId, boolean online, Instant lastSeenAt) {
        return PresenceStatusCommand.builder()
                .userId(userId)
                .online(online)
                .lastSeenAt(lastSeenAt)
                .build();
    }

    private record SessionPresence(UUID userId, Instant lastHeartbeatAt) { }
}
