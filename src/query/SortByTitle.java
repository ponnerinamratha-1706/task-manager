package query;

import model.Task;

import java.util.Comparator;

public class SortByTitle implements SortStrategy {
    @Override public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }
}
