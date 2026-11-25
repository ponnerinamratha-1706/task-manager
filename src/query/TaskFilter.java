package query;

import model.Task;

/** One filtering rule. Add a new filter by writing a new class; nothing existing changes. */
public interface TaskFilter {
    boolean matches(Task task);
}
