package event;

import model.Task;

import java.util.Map;
import java.util.TreeMap;

/** Keeps a count of each event type. */
public class AuditTrailListener implements TaskEventListener {
    private final Map<String, Integer> counts = new TreeMap<>();

    @Override
    public synchronized void onEvent(String eventType, Task task) {
        counts.merge(eventType, 1, Integer::sum);
    }

    public synchronized int getCount(String eventType) { return counts.getOrDefault(eventType, 0); }

    public synchronized String summary() { return counts.toString(); }
}
