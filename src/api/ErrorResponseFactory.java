package api;

import exception.ApiException;
import exception.FieldIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The ONE place that builds the error shape. */
public final class ErrorResponseFactory {
    private ErrorResponseFactory() {}

    public static ApiResponse from(ApiException e) {
        return build(e.getStatus(), e.getCode(), e.getMessage(), e.getDetails());
    }

    public static ApiResponse fromUnexpected(Exception e) {
        // Do not leak internal messages to the client.
        return build(500, "INTERNAL_ERROR", "Unexpected error", List.of());
    }

    private static ApiResponse build(int status, String code, String message, List<FieldIssue> details) {
        List<Map<String, Object>> detailList = new ArrayList<>();
        for (FieldIssue d : details) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("field", d.getField());
            m.put("issue", d.getIssue());
            detailList.add(m);
        }
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("details", detailList);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        return new ApiResponse(status, body);
    }
}
