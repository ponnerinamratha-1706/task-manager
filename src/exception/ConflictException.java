package exception;

/** 422: an idempotency key was reused with a different request. */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(422, "IDEMPOTENCY_CONFLICT", message);
    }
}
