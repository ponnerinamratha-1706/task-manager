package service;

import exception.FieldIssue;
import exception.ValidationException;
import model.Priority;
import model.Task;
import model.TaskRequest;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** All input rules live here. Used for client input AND for anything the model returns. */
public final class TaskValidator {
    private static final Set<String> RECURRENCES = Set.of("daily", "weekly", "monthly");

    private TaskValidator() {}

    /** @return a builder with validated values; @throws ValidationException listing every problem */
    public static Task.Builder validate(TaskRequest r) {
        if (r == null) throw new ValidationException("request", "must not be null");

        List<FieldIssue> issues = new ArrayList<>();

        if (r.getTitle() == null || r.getTitle().isBlank()) issues.add(new FieldIssue("title", "must not be empty"));
        if (r.getDescription() == null || r.getDescription().isBlank()) issues.add(new FieldIssue("description", "must not be empty"));

        String c = r.getCompleted();
        if (!"true".equals(c) && !"false".equals(c)) issues.add(new FieldIssue("completed", "must be exactly \"true\" or \"false\""));

        LocalDate due = null;
        if (r.getDueDate() != null) {
            due = parseDate(r.getDueDate());
            if (due == null) issues.add(new FieldIssue("dueDate", "must be a real date in YYYY-MM-DD format"));
        }

        Priority priority = null;
        if (r.getPriority() != null) {
            try {
                priority = Priority.valueOf(r.getPriority());
            } catch (IllegalArgumentException e) {
                issues.add(new FieldIssue("priority", "must be LOW, MEDIUM, or HIGH"));
            }
        }

        if (r.getRecurrence() != null && !RECURRENCES.contains(r.getRecurrence())) {
            issues.add(new FieldIssue("recurrence", "must be daily, weekly, or monthly"));
        }

        if (!issues.isEmpty()) throw new ValidationException(issues);

        return new Task.Builder(r.getTitle().trim(), r.getDescription().trim(), Boolean.parseBoolean(c))
                .dueDate(due)
                .priority(priority)
                .recurrence(r.getRecurrence());
    }

    /** Returns null when the text is not a real YYYY-MM-DD date (2026-02-30 -> null). */
    static LocalDate parseDate(String text) {
        if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        try {
            return LocalDate.parse(text); // strict: checks month length and leap years
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
