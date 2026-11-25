package api;

import exception.ApiException;
import model.Task;
import model.TaskRequest;
import query.PageResult;
import query.TaskQuery;
import service.TaskService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Public operations. Never throws: every outcome is an ApiResponse. */
public class TaskApi {
    private static final String SEP = "\u001F";

    private final TaskService service;
    private final IdempotencyStore idempotency;

    public TaskApi(TaskService service, IdempotencyStore idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    // ---- create ----
    public ApiResponse createTask(TaskRequest request) { return createTask(request, null); }

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        String fp = "createTask" + SEP + (request == null ? "null" : request.fingerprint());
        return run(() -> idempotency.execute(idempotencyKey, fp,
                () -> respond(201, service.createTask(request))));
    }

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        String fp = "createTaskFromText" + SEP + text;
        return run(() -> idempotency.execute(idempotencyKey, fp,
                () -> respond(201, service.createTaskFromText(text))));
    }

    // ---- read ----
    public ApiResponse getTask(int id) { return run(() -> respond(200, service.getTask(id))); }

    public ApiResponse listTasks(TaskQuery query) {
        return run(() -> {
            TaskQuery q = query != null ? query : new TaskQuery.Builder().build();
            PageResult page = service.listTasks(q);
            return new ApiResponse(200, page.toMap());
        });
    }

    // ---- update / complete / delete ----
    public ApiResponse updateTask(int id, TaskRequest request) {
        return run(() -> respond(200, service.updateTask(id, request)));
    }

    public ApiResponse completeTask(int id) { return run(() -> respond(200, service.completeTask(id))); }

    public ApiResponse deleteTask(int id) {
        return run(() -> {
            List<Integer> deleted = service.deleteTask(id);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("deletedIds", deleted);
            return new ApiResponse(200, body);
        });
    }

    // ---- subtasks ----
    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        return run(() -> respond(201, service.createSubtask(parentId, request)));
    }

    public ApiResponse listSubtasks(int parentId) {
        return run(() -> list(200, service.listSubtasks(parentId)));
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        String fp = "breakDownTask" + SEP + id;
        return run(() -> idempotency.execute(idempotencyKey, fp,
                () -> list(201, service.breakDownTask(id))));
    }

    // ---- helpers ----
    /** Single try/catch used by every operation: ApiException -> its status, anything else -> 500. */
    private ApiResponse run(Supplier<ApiResponse> operation) {
        try {
            return operation.get();
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    private static ApiResponse respond(int status, Task task) { return new ApiResponse(status, task.toMap()); }

    private static ApiResponse list(int status, List<Task> tasks) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Task t : tasks) items.add(t.toMap());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("count", items.size());
        return new ApiResponse(status, body);
    }
}
