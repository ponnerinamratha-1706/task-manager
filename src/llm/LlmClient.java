package llm;

/**
 * Sends a prompt to a language model and returns the raw text reply.
 * NOTE: the instructor supplies the real client. If theirs has a different method name,
 * change this interface (and the places that call it) to match.
 */
public interface LlmClient {
    /** @throws LlmException if the call fails or times out */
    String complete(String prompt);
}
