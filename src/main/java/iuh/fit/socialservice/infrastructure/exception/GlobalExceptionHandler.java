package iuh.fit.socialservice.infrastructure.exception;

import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.exception.ForbiddenException;
import iuh.fit.socialservice.application.dto.response.base.BaseResponse;
import iuh.fit.socialservice.application.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<BaseResponse<Object>> handleUnauthenticated(UnauthenticatedException exception) {
        return errorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), "Chưa đăng nhập");
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<BaseResponse<Object>> handleForbidden(ForbiddenException exception) {
        return errorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), "Không có quyền");
    }

    /** @PreAuthorize từ chối: không để rơi xuống handler Exception chung thành 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<BaseResponse<Object>> handleAccessDenied(AccessDeniedException exception) {
        return errorResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", "Không có quyền");
    }

    @ExceptionHandler(ServiceException.class)
    ResponseEntity<BaseResponse<Object>> handleServiceException(ServiceException exception) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                "Yêu cầu không hợp lệ"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<BaseResponse<Object>> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        Object message = messages.isEmpty()
                ? "Không tìm thấy dữ liệu hợp lệ"
                : messages.size() == 1 ? messages.getFirst() : messages;
        return errorResponse(HttpStatus.BAD_REQUEST, message, "Dữ liệu không hợp lệ");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<BaseResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        List<String> messages = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .toList();
        Object message = messages.size() == 1 ? messages.getFirst() : messages;
        return errorResponse(HttpStatus.BAD_REQUEST, message, "Dữ liệu không hợp lệ");
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<BaseResponse<Object>> handleMalformedRequestException(Exception exception) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không đúng định dạng.",
                "Yêu cầu không hợp lệ"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<BaseResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Data conflict while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );
        return errorResponse(
                HttpStatus.CONFLICT,
                "Dữ liệu đã tồn tại hoặc đang được sử dụng.",
                "Xung đột dữ liệu"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<BaseResponse<Object>> handleResourceException(NoResourceFoundException exception) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy đường dẫn được yêu cầu.",
                "Đường dẫn không tồn tại"
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<BaseResponse<Object>> handleAllExceptions(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.",
                "Có lỗi xảy ra"
        );
    }

    private ResponseEntity<BaseResponse<Object>> errorResponse(
            HttpStatus status,
            Object message,
            String error
    ) {
        BaseResponse<Object> response = new BaseResponse<>();
        response.setStatusCode(status.value());
        response.setMessage(message);
        response.setError(error);
        return ResponseEntity.status(status).body(response);
    }
}
