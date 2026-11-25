package model;

/** Raw client input: everything is a String so validation decides what is acceptable. */
public class TaskRequest {
    private final String title;
    private final String description;
    private final String completed;
    private final String dueDate;
    private final String priority;
    private String recurrence; // optional, set by the AI parsers

    public TaskRequest(String title, String description, String completed, String dueDate, String priority) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCompleted() { return completed; }
    public String getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }

    /** All field values joined into one String, used for idempotency checks. */
    public String fingerprint() {
        return String.join("\u001F", String.valueOf(title), String.valueOf(description),
                String.valueOf(completed), String.valueOf(dueDate), String.valueOf(priority),
                String.valueOf(recurrence));
    }
}
