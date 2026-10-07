package iuh.fit.socialservice.infrastructure.websocket;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record WebSocketUserPrincipal(UUID userId, Set<String> roles) implements Principal {

    public WebSocketUserPrincipal(UUID userId) {
        this(userId, Set.of());
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
