package llm;

/** Test double that misbehaves in a chosen way. */
public class FaultyLlmClient implements LlmClient {
    public enum Mode { EMPTY_TITLE, IMPOSSIBLE_DATE, MALFORMED, WRONG_TYPE, THROWS }

    private final Mode mode;
    private int calls = 0;

    public FaultyLlmClient(Mode mode) { this.mode = mode; }

    @Override
    public String complete(String prompt) {
        calls++;
        return switch (mode) {
            case EMPTY_TITLE -> "{\"title\":\"\",\"description\":\"x\",\"dueDate\":null,\"recurrence\":null}";
            case IMPOSSIBLE_DATE -> "{\"title\":\"Pay rent\",\"description\":\"x\",\"dueDate\":\"2026-02-30\",\"recurrence\":null}";
            case MALFORMED -> "Sure! Here is your task: title is pay rent";
            case WRONG_TYPE -> "{\"title\":123,\"description\":\"x\",\"dueDate\":null,\"recurrence\":null}";
            case THROWS -> throw new LlmException("simulated timeout");
        };
    }

    public int getCalls() { return calls; }
}
