package exception;

import java.util.List;

/** 400: the client's input broke a rule. Can report several field issues at once. */
public class ValidationException extends ApiException {
    public ValidationException(List<FieldIssue> issues) {
        super(400, "VALIDATION_ERROR", "Validation failed", issues);
    }

    public ValidationException(String field, String issue) {
        this(List.of(new FieldIssue(field, issue)));
    }
}
