package parser;

import exception.ServiceUnavailableException;
import llm.HttpLlmClient;
import llm.LlmClient;
import util.EnvironmentConfig;

/**
 * Factory + Strategy: decides which parser / breakdown strategy the service receives.
 * The service never knows (or cares) which one it got.
 */
public final class ParserFactory {
    private ParserFactory() {}

    private static String apiKey() {
        return EnvironmentConfig.get("LLM_API_KEY");
    }

    public static boolean isLlmConfigured() { return apiKey() != null; }

    /**
     * @param preferOffline true = rule-based parser.
     *                      false = LLM parser; if LLM_API_KEY is missing, every call answers 503 (no crash).
     */
    public static TaskParser createParser(boolean preferOffline) {
        if (preferOffline) return new RuleBasedTaskParser();
        String key = apiKey();
        if (key == null) {
            return (text, today) -> { throw new ServiceUnavailableException("LLM_API_KEY is not set"); };
        }
        return new LlmTaskParser(new HttpLlmClient(key));
    }

    /** Plug in any client (used for FakeLlmClient / FaultyLlmClient in tests). */
    public static TaskParser createParser(LlmClient client) { return new LlmTaskParser(client); }

    public static BreakdownStrategy createBreakdown(boolean preferOffline) {
        if (preferOffline) return new RuleBasedBreakdownStrategy();
        String key = apiKey();
        if (key == null) {
            return task -> { throw new ServiceUnavailableException("LLM_API_KEY is not set"); };
        }
        return new LlmBreakdownStrategy(new HttpLlmClient(key));
    }

    public static BreakdownStrategy createBreakdown(LlmClient client) { return new LlmBreakdownStrategy(client); }
}
