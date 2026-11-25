package service;

import event.TaskEventListener;
import exception.BadGatewayException;
import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.Task;
import model.TaskRequest;
import parser.BreakdownStrategy;
import parser.ParsedTask;
import parser.TaskParser;
import query.PageResult;
import query.TaskQuery;
import repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Business logic. Depends only on interfaces (Dependency Inversion). Also the Observer "subject". */
public class TaskService {
    private final TaskRepository repository;
    private final TaskParser parser;
    private final BreakdownStrategy breakdown;
    private final Supplier<LocalDate> today;
    private final List<TaskEventListener> listeners = new CopyOnWriteArrayList<>();

    public TaskService(TaskRepository repository, TaskParser parser, BreakdownStrategy breakdown) {
        this(repository, parser, breakdown, LocalDate::now);
    }

    /** The clock is injectable so tests can fix "today". */
    public TaskService(TaskRepository repository, TaskParser parser, BreakdownStrategy breakdown, Supplier<LocalDate> today) {
        this.repository = repository;
        this.parser = parser;
        this.breakdown = breakdown;
        this.today = today;
    }

    // ---------- Observer ----------
    public void addListener(TaskEventListener l) { listeners.add(l); }
    public void removeListener(TaskEventListener l) { listeners.remove(l); }

    private void fire(String type, Task task) {
        for (TaskEventListener l : listeners) l.onEvent(type, task);
    }

    // ---------- CRUD ----------
    public Task createTask(TaskRequest request) {
        Task saved = repository.save(TaskValidator.validate(request).build());
        fire(TaskEventListener.CREATED, saved);
        return saved;
    }

    public Task getTask(int id) { return requireTask(id); }

    public Task updateTask(int id, TaskRequest request) {
        Task existing = requireTask(id);
        String recurrence = request != null && request.getRecurrence() != null ? request.getRecurrence() : existing.getRecurrence();
        Task updated = repository.save(TaskValidator.validate(request)
                .id(id)
                .parentId(existing.getParentId())
                .recurrence(recurrence)
                .build());
        fire(TaskEventListener.UPDATED, updated);
        return updated;
    }

    public Task completeTask(int id) {
        Task existing = requireTask(id);
        Task done = repository.save(existing.toBuilder().completed(true).build());
        fire(TaskEventListener.COMPLETED, done);
        return done;
    }

    /** Deletes the task and all of its subtasks. Returns the deleted ids (subtasks first). */
    public List<Integer> deleteTask(int id) {
        Task task = requireTask(id);
        List<Integer> deleted = new ArrayList<>();
        for (Task sub : subtasksOf(id)) {
            repository.delete(sub.getId());
            deleted.add(sub.getId());
            fire(TaskEventListener.DELETED, sub);
        }
        repository.delete(id);
        deleted.add(id);
        fire(TaskEventListener.DELETED, task);
        return deleted;
    }

    // ---------- AI creation ----------
    public Task createTaskFromText(String text) {
        if (text == null || text.isBlank()) throw new ValidationException("text", "must not be empty");

        ParsedTask parsed = parser.parse(text, today.get().toString());
        if (parsed == null) throw new BadGatewayException("The parser returned nothing");

        // "Never trust the model": run its output through the SAME validation as client input.
        TaskRequest request = new TaskRequest(parsed.getTitle(), parsed.getDescription(), "false", parsed.getDueDate(), null);
        request.setRecurrence(parsed.getRecurrence());
        Task.Builder builder;
        try {
            builder = TaskValidator.validate(request);
        } catch (ValidationException e) {
            throw new BadGatewayException("The parser produced an invalid task", e.getDetails());
        }
        Task saved = repository.save(builder.build());
        fire(TaskEventListener.CREATED, saved);
        return saved;
    }

    // ---------- Subtasks ----------
    public Task createSubtask(int parentId, TaskRequest request) {
        Task parent = requireTask(parentId);
        if (parent.getParentId() != null) {
            throw new ValidationException("parentId", "a subtask cannot have subtasks");
        }
        Task saved = repository.save(TaskValidator.validate(request).parentId(parentId).build());
        fire(TaskEventListener.CREATED, saved);
        return saved;
    }

    public List<Task> listSubtasks(int parentId) {
        requireTask(parentId);
        return subtasksOf(parentId);
    }

    /** All-or-nothing: every proposed subtask is validated BEFORE anything is saved. */
    public List<Task> breakDownTask(int id) {
        Task parent = requireTask(id);
        if (parent.getParentId() != null) {
            throw new ValidationException("id", "a subtask cannot be broken down");
        }
        List<TaskRequest> proposals = breakdown.breakDown(parent);
        if (proposals == null || proposals.isEmpty()) {
            throw new BadGatewayException("The breakdown strategy returned no subtasks");
        }

        List<Task> toSave = new ArrayList<>();
        List<FieldIssue> issues = new ArrayList<>();
        for (int i = 0; i < proposals.size(); i++) {
            try {
                toSave.add(TaskValidator.validate(proposals.get(i)).parentId(id).build());
            } catch (ValidationException e) {
                for (FieldIssue d : e.getDetails()) {
                    issues.add(new FieldIssue("subtasks[" + i + "]." + d.getField(), d.getIssue()));
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new BadGatewayException("The breakdown produced invalid subtasks; none were saved", issues);
        }

        List<Task> saved = new ArrayList<>();
        for (Task t : toSave) {
            Task s = repository.save(t);
            saved.add(s);
            fire(TaskEventListener.CREATED, s);
        }
        return saved;
    }

    // ---------- Query ----------
    public PageResult listTasks(TaskQuery q) {
        List<FieldIssue> issues = new ArrayList<>();
        if (q.getPage() < 1) issues.add(new FieldIssue("page", "must be at least 1"));
        if (q.getPageSize() < 1 || q.getPageSize() > 100) issues.add(new FieldIssue("pageSize", "must be between 1 and 100"));
        if (!issues.isEmpty()) throw new ValidationException(issues);

        Comparator<Task> cmp = q.getSort().comparator();
        if (q.isDescending()) cmp = cmp.reversed();

        List<Task> matched = repository.findAll().stream()
                .filter(t -> q.getFilters().stream().allMatch(f -> f.matches(t)))
                .sorted(cmp)
                .collect(Collectors.toList());

        int total = matched.size();
        int pageSize = q.getPageSize();
        int totalPages = (total + pageSize - 1) / pageSize;
        long from = (long) (q.getPage() - 1) * pageSize;
        List<Task> items = from >= total
                ? List.of()                                   // page beyond the last page: empty, not an error
                : matched.subList((int) from, (int) Math.min(total, from + pageSize));
        return new PageResult(items, q.getPage(), pageSize, total, totalPages);
    }

    // ---------- helpers ----------
    private Task requireTask(int id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
    }

    private List<Task> subtasksOf(int parentId) {
        return repository.findAll().stream()
                .filter(t -> t.getParentId() != null && t.getParentId() == parentId)
                .collect(Collectors.toList());
    }
}
