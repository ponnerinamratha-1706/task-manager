package exception;

import java.util.List;

/** 502: the LLM failed, timed out, or returned something we refuse to trust. */
public class BadGatewayException extends ApiException {
    public BadGatewayException(String message) {
        super(502, "BAD_GATEWAY", message);
    }

    public BadGatewayException(String message, List<FieldIssue> details) {
        super(502, "BAD_GATEWAY", message, details);
    }
}
