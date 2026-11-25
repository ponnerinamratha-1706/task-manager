package query;

import model.Priority;
import model.Task;

public class PriorityFilter implements TaskFilter {
    private final Priority priority;
    public PriorityFilter(Priority priority) { this.priority = priority; }
    @Override public boolean matches(Task t) { return t.getPriority() == priority; }
}
