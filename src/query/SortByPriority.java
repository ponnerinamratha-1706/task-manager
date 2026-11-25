package query;

import model.Task;

import java.util.Comparator;

/** HIGH first, then MEDIUM, then LOW. */
public class SortByPriority implements SortStrategy {
    @Override public Comparator<Task> comparator() {
        return Comparator.comparingInt(t -> -t.getPriority().ordinal());
    }
}
