package exception;

/** 503: a dependency is not configured (for example, LLM_API_KEY is missing). */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(503, "SERVICE_UNAVAILABLE", message);
    }
}
