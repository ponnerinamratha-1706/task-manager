package llm;

import util.JsonWriter;
import util.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stand-in for the instructor's client: calls the Anthropic Messages API with java.net.http. */
public class HttpLlmClient implements LlmClient {
    private final String apiKey;
    private final String model;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public HttpLlmClient(String apiKey) {
        this.apiKey = apiKey;
        String m = System.getenv("LLM_MODEL");
        this.model = (m == null || m.isBlank()) ? "claude-sonnet-5-5" : m;
    }

    @Override
    public String complete(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 600);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(JsonWriter.write(body, false)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new LlmException("LLM provider returned HTTP " + res.statusCode());
            Map<String, Object> json = SimpleJson.parseObject(res.body());
            Object content = json.get("content");
            if (content instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first
                    && first.get("text") instanceof String text) {
                return text;
            }
            throw new LlmException("Unexpected response shape from LLM provider");
        } catch (IOException e) {
            throw new LlmException("LLM call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM call interrupted", e);
        } catch (IllegalArgumentException e) {
            throw new LlmException("Could not read LLM response", e);
        }
    }
}
