package api;

import util.JsonWriter;

import java.util.Map;

/** Status code + JSON-style body. (Stand-in for the instructor's class; same idea.) */
public class ApiResponse {
    private final int status;
    private final Map<String, Object> body;

    public ApiResponse(int status, Map<String, Object> body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() { return status; }
    public Map<String, Object> getBody() { return body; }

    public String toJson() { return JsonWriter.write(body, true); }

    @Override
    public String toString() { return status + " " + JsonWriter.write(body, false); }
}
