package llm;

/** Test double: always returns the same reply and counts how often it was called. */
public class FakeLlmClient implements LlmClient {
    private final String reply;
    private int calls = 0;

    public FakeLlmClient(String reply) { this.reply = reply; }

    @Override
    public String complete(String prompt) {
        calls++;
        return reply;
    }

    public int getCalls() { return calls; }
}
