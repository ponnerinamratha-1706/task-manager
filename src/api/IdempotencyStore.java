package api;

import exception.ConflictException;
import exception.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Singleton. Remembers successful responses by idempotency key. Thread-safe (synchronized). */
public class IdempotencyStore {
    private static final IdempotencyStore INSTANCE = new IdempotencyStore();

    private static final class Record {
        final String fingerprint;
        final ApiResponse response;
        Record(String fingerprint, ApiResponse response) {
            this.fingerprint = fingerprint;
            this.response = response;
        }
    }

    private final Map<String, Record> records = new HashMap<>();

    private IdempotencyStore() {}

    public static IdempotencyStore getInstance() { return INSTANCE; }

    /**
     * No key: just run the action.
     * Same key + same fingerprint: return the stored response (action NOT run).
     * Same key + different fingerprint: 422.
     * Only 2xx responses are stored, so a client can fix bad input and retry with the same key.
     */
    public synchronized ApiResponse execute(String key, String fingerprint, Supplier<ApiResponse> action) {
        if (key == null) return action.get();
        if (key.isBlank()) throw new ValidationException("idempotencyKey", "must not be blank when provided");

        Record existing = records.get(key);
        if (existing != null) {
            if (existing.fingerprint.equals(fingerprint)) return existing.response;
            throw new ConflictException("Idempotency key was already used with a different request");
        }
        ApiResponse response = action.get();
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            records.put(key, new Record(fingerprint, response));
        }
        return response;
    }

    public synchronized void clear() { records.clear(); }
    public synchronized int size() { return records.size(); }
}
