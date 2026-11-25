package query;

import model.Task;

import java.time.LocalDate;
import java.util.Comparator;

/** Earliest first; tasks without a due date go last. */
public class SortByDueDate implements SortStrategy {
    @Override public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()));
    }
}
