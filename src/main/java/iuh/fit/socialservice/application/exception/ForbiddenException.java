package iuh.fit.socialservice.application.exception;

/** Nguoi goi da dang nhap nhung khong co vai tro can thiet. Tra HTTP 403. */
public class ForbiddenException extends ServiceException {
    public ForbiddenException(String message) {
        super(message);
    }
}
