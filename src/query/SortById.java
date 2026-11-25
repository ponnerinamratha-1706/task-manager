package query;

import model.Task;

import java.util.Comparator;

public class SortById implements SortStrategy {
    @Override public Comparator<Task> comparator() { return Comparator.comparingInt(Task::getId); }
}
