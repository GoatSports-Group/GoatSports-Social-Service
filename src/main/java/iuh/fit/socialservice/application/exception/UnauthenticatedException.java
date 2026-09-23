package iuh.fit.socialservice.application.exception;

/**
 * The caller has no valid session (missing or expired access token). Mapped to HTTP 401 so the
 * clients' interceptor refreshes the token and replays the request instead of failing the page.
 */
public class UnauthenticatedException extends ServiceException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
