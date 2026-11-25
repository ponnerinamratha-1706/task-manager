package query;

import model.Task;

public class CompletedFilter implements TaskFilter {
    private final boolean completed;
    public CompletedFilter(boolean completed) { this.completed = completed; }
    @Override public boolean matches(Task t) { return t.isCompleted() == completed; }
}
