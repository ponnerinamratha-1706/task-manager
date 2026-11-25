package parser;

/**
 * Strategy interface: turns free text into a ParsedTask.
 * Implementations may throw ApiException (for example BadGatewayException) on failure.
 */
public interface TaskParser {
    /** @param today the current date as YYYY-MM-DD, so relative dates can be resolved */
    ParsedTask parse(String text, String today);
}
