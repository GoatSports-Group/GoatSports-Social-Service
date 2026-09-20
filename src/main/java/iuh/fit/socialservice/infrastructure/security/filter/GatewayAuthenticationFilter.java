package iuh.fit.socialservice.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private final byte[] gatewaySecret;

    public GatewayAuthenticationFilter(@Value("${app.gateway.secret}") String gatewaySecret) {
        this.gatewaySecret = gatewaySecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String suppliedSecret = request.getHeader("X-Gateway-Secret");
        if (!isValidSecret(suppliedSecret)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"statusCode\":403,\"message\":\"Social API must be called through API Gateway\"}"
            );
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String principal = firstNonBlank(
                request.getHeader("X-User-Email"),
                request.getHeader("X-User-Username"),
                "gateway"
        );
        List<SimpleGrantedAuthority> authorities = Stream.concat(
                        split(request.getHeader("X-User-Authorities")),
                        split(request.getHeader("X-User-Roles"))
                                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                )
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isValidSecret(String suppliedSecret) {
        return suppliedSecret != null && MessageDigest.isEqual(
                gatewaySecret,
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("gateway");
    }

    private Stream<String> split(String raw) {
        return raw == null
                ? Stream.empty()
                : Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank());
    }
}
