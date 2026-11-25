package exception;

import java.util.List;

/** Base class: every failure the API reports carries an HTTP-style status and a string error code. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<FieldIssue> details;

    public ApiException(int status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(int status, String code, String message, List<FieldIssue> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = List.copyOf(details);
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public List<FieldIssue> getDetails() { return details; }
}
