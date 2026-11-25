package query;

import model.Task;

import java.util.Comparator;

/** One sort order. Add a new one by writing a new class. */
public interface SortStrategy {
    Comparator<Task> comparator();
}
