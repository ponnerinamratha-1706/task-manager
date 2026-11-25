package query;

import model.Task;

import java.time.LocalDate;

/** Matches tasks that have a due date on or before the given date. */
public class DueBeforeFilter implements TaskFilter {
    private final LocalDate date;
    public DueBeforeFilter(LocalDate date) { this.date = date; }
    @Override public boolean matches(Task t) { return t.getDueDate() != null && !t.getDueDate().isAfter(date); }
}
