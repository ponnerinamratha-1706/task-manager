package model;

import util.JsonWriter;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable task. Create it only through Task.Builder. */
public final class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final LocalDate dueDate;     // optional
    private final Priority priority;
    private final Integer parentId;      // optional (null = top-level task)
    private final String recurrence;     // optional

    private Task(Builder b) {
        this.id = b.id;
        this.title = b.title;
        this.description = b.description;
        this.completed = b.completed;
        this.dueDate = b.dueDate;
        this.priority = b.priority;
        this.parentId = b.parentId;
        this.recurrence = b.recurrence;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public LocalDate getDueDate() { return dueDate; }
    public Priority getPriority() { return priority; }
    public Integer getParentId() { return parentId; }
    public String getRecurrence() { return recurrence; }

    /** Copy into a builder so you can "change" an immutable task by building a new one. */
    public Builder toBuilder() {
        return new Builder(title, description, completed)
                .id(id).dueDate(dueDate).priority(priority).parentId(parentId).recurrence(recurrence);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", description);
        m.put("completed", completed);
        m.put("dueDate", dueDate == null ? null : dueDate.toString());
        m.put("priority", priority.name());
        m.put("parentId", parentId);
        m.put("recurrence", recurrence);
        return m;
    }

    @Override
    public String toString() { return JsonWriter.write(toMap(), false); }

    public static class Builder {
        private int id;
        private final String title;
        private final String description;
        private boolean completed;
        private LocalDate dueDate;
        private Priority priority = Priority.MEDIUM;
        private Integer parentId;
        private String recurrence;

        /** Required fields go in the constructor; everything else is optional. */
        public Builder(String title, String description, boolean completed) {
            this.title = title;
            this.description = description;
            this.completed = completed;
        }

        public Builder id(int id) { this.id = id; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder priority(Priority priority) { this.priority = priority == null ? Priority.MEDIUM : priority; return this; }
        public Builder parentId(Integer parentId) { this.parentId = parentId; return this; }
        public Builder recurrence(String recurrence) { this.recurrence = recurrence; return this; }
        public Task build() { return new Task(this); }
    }
}
