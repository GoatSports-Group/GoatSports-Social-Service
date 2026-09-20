package iuh.fit.socialservice.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class GatewayHandshakeHandler extends DefaultHandshakeHandler {

    static final String USER_ID_ATTRIBUTE = "goatsports.userId";

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                      @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {
        Object userId = attributes.get(USER_ID_ATTRIBUTE);
        return userId instanceof UUID id ? new WebSocketUserPrincipal(id) : null;
    }

    /** Đọc `X-User-Id` từ request nâng cấp và cất vào thuộc tính phiên. */
    public static class UserIdInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                       @NonNull ServerHttpResponse response,
                                       @NonNull WebSocketHandler wsHandler,
                                       @NonNull Map<String, Object> attributes) {
            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                return false;
            }
            String raw = servletRequest.getServletRequest().getHeader("X-User-Id");
            if (raw == null || raw.isBlank()) {
                log.warn("Từ chối bắt tay WebSocket: thiếu X-User-Id");
                return false;
            }
            try {
                attributes.put(USER_ID_ATTRIBUTE, UUID.fromString(raw));
                return true;
            } catch (IllegalArgumentException exception) {
                log.warn("Từ chối bắt tay WebSocket: X-User-Id không hợp lệ");
                return false;
            }
        }

        @Override
        public void afterHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   Exception exception) {
            // Không cần dọn gì.
        }
    }
}
