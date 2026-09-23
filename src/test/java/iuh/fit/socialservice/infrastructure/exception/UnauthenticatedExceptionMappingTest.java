package iuh.fit.socialservice.infrastructure.exception;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A missing/expired session must answer 401 (clients refresh the token and replay the request on 401 only);
 * an ordinary ServiceException stays 400.
 */
class UnauthenticatedExceptionMappingTest {
    private final ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

    @Test
    void unauthenticatedResolvesToThe401HandlerNotTheServiceExceptionOne() {
        assertThat(resolver.resolveMethod(new UnauthenticatedException("Phiên đăng nhập hết hạn")).getName())
                .isEqualTo("handleUnauthenticated");
        assertThat(resolver.resolveMethod(new ServiceException("Dữ liệu không hợp lệ")).getName())
                .isNotEqualTo("handleUnauthenticated");
    }
}
