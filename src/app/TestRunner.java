package app;

import api.ApiResponse;
import api.IdempotencyStore;
import api.TaskApi;
import event.AuditTrailListener;
import event.TaskEventListener;
import llm.FakeLlmClient;
import llm.FaultyLlmClient;
import llm.FaultyLlmClient.Mode;
import llm.LlmException;
import model.Priority;
import model.TaskRequest;
import parser.BreakdownStrategy;
import parser.ParserFactory;
import parser.TaskParser;
import query.SortByPriority;
import query.TaskQuery;
import repository.InMemoryTaskRepository;
import service.TaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Runs without an API key or internet: all model calls use fakes. Run it and read PASS/FAIL. */
public class TestRunner {
    static int passed = 0, failed = 0;
    static TaskService service;
    static final InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();

    static final String GOOD_TASK = "{\"title\":\"Pay rent\",\"description\":\"Pay monthly rent\",\"dueDate\":\"2026-12-01\",\"recurrence\":\"monthly\"}";
    static final String GOOD_SUBTASKS = "Find hotel|Compare prices\nBook flights|Check dates\nPack bags|Make a list";

    static TaskApi newApi(TaskParser parser, BreakdownStrategy breakdown) {
        repo.clear();
        IdempotencyStore.getInstance().clear();
        service = new TaskService(repo, parser, breakdown, () -> LocalDate.of(2026, 11, 25));
        return new TaskApi(service, IdempotencyStore.getInstance());
    }

    static TaskApi newApi() {
        return newApi(ParserFactory.createParser(true), ParserFactory.createBreakdown(true));
    }

    static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
    }

    static TaskRequest req(String title) { return new TaskRequest(title, "desc", "false", null, null); }

    @SuppressWarnings("unchecked")
    static Map<String, Object> error(ApiResponse r) { return (Map<String, Object>) r.getBody().get("error"); }
    static String code(ApiResponse r) { return String.valueOf(error(r).get("code")); }
    static int detailCount(ApiResponse r) { return ((List<?>) error(r).get("details")).size(); }
    static int count() { return repo.findAll().size(); }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> items(ApiResponse r) { return (List<Map<String, Object>>) r.getBody().get("items"); }

    public static void main(String[] args) {
        // ---------- CRUD ----------
        TaskApi api = newApi();
        ApiResponse r = api.createTask(new TaskRequest("Buy milk", "2 litres", "false", "2026-12-01", "HIGH"));
        check("create valid task -> 201", r.getStatus() == 201 && "Buy milk".equals(r.getBody().get("title")));
        check("get existing -> 200", api.getTask(1).getStatus() == 200);
        check("get unknown id -> 404 NOT_FOUND", api.getTask(99).getStatus() == 404 && code(api.getTask(99)).equals("NOT_FOUND"));
        check("update -> 200 and title changed", "New".equals(api.updateTask(1, req("New")).getBody().get("title")));
        check("update unknown -> 404", api.updateTask(99, req("x")).getStatus() == 404);
        check("complete -> completed true", Boolean.TRUE.equals(api.completeTask(1).getBody().get("completed")));
        check("complete unknown -> 404", api.completeTask(99).getStatus() == 404);
        check("delete -> 200 then get -> 404", api.deleteTask(1).getStatus() == 200 && api.getTask(1).getStatus() == 404);
        check("delete unknown -> 404", api.deleteTask(1).getStatus() == 404);

        // ---------- Validation ----------
        api = newApi();
        r = api.createTask(req(""));
        check("empty title -> 400 VALIDATION_ERROR", r.getStatus() == 400 && code(r).equals("VALIDATION_ERROR"));
        check("blank description -> 400", api.createTask(new TaskRequest("a", "   ", "false", null, null)).getStatus() == 400);
        check("completed 'yes' -> 400", api.createTask(new TaskRequest("a", "b", "yes", null, null)).getStatus() == 400);
        check("completed 'TRUE' -> 400 (exact match only)", api.createTask(new TaskRequest("a", "b", "TRUE", null, null)).getStatus() == 400);
        check("2026-02-30 -> 400", api.createTask(new TaskRequest("a", "b", "false", "2026-02-30", null)).getStatus() == 400);
        check("2026-02-29 (not a leap year) -> 400", api.createTask(new TaskRequest("a", "b", "false", "2026-02-29", null)).getStatus() == 400);
        check("2024-02-29 (leap year) -> 201", api.createTask(new TaskRequest("a", "b", "false", "2024-02-29", null)).getStatus() == 201);
        check("dueDate '1/2/2026' -> 400", api.createTask(new TaskRequest("a", "b", "false", "1/2/2026", null)).getStatus() == 400);
        check("priority URGENT -> 400", api.createTask(new TaskRequest("a", "b", "false", null, "URGENT")).getStatus() == 400);
        check("priority lowercase 'high' -> 400", api.createTask(new TaskRequest("a", "b", "false", null, "high")).getStatus() == 400);
        r = api.createTask(new TaskRequest(" ", "", "yes", "2026-02-30", "URGENT"));
        check("several problems reported at once (5 details)", r.getStatus() == 400 && detailCount(r) == 5);
        check("null request -> 400, not a crash", api.createTask(null).getStatus() == 400);
        check("error code and message are Strings", error(r).get("code") instanceof String && error(r).get("message") instanceof String);

        // ---------- Idempotency ----------
        api = newApi();
        ApiResponse first = api.createTask(req("Pay rent"), "k1");
        ApiResponse second = api.createTask(req("Pay rent"), "k1");
        check("same key + same request -> same response, one task",
                first.getStatus() == 201 && second == first && count() == 1);
        r = api.createTask(req("Different"), "k1");
        check("same key + different request -> 422", r.getStatus() == 422 && count() == 1);
        api.createTask(req(""), "k2");
        check("failed request is not stored: fixed retry with same key works",
                api.createTask(req("Fixed"), "k2").getStatus() == 201 && count() == 2);
        check("no key -> two separate tasks", api.createTask(req("same")).getStatus() == 201
                && api.createTask(req("same")).getStatus() == 201 && count() == 4);
        check("blank key -> 400", api.createTask(req("a"), "  ").getStatus() == 400);
        check("same key across different operations -> 422", api.createTaskFromText("Call mom", "k1").getStatus() == 422);

        // ---------- AI creation with fakes ----------
        FakeLlmClient fake = new FakeLlmClient(GOOD_TASK);
        api = newApi(ParserFactory.createParser(fake), ParserFactory.createBreakdown(true));
        r = api.createTaskFromText("Remind me to pay rent on the 1st of every month", "ai1");
        check("LLM path: valid reply -> 201", r.getStatus() == 201 && "monthly".equals(r.getBody().get("recurrence")));
        api.createTaskFromText("Remind me to pay rent on the 1st of every month", "ai1");
        check("repeated key never calls the fake twice, one task", fake.getCalls() == 1 && count() == 1);
        check("blank text -> 400", api.createTaskFromText("  ", null).getStatus() == 400);

        for (Mode mode : Mode.values()) {
            FaultyLlmClient bad = new FaultyLlmClient(mode);
            api = newApi(ParserFactory.createParser(bad), ParserFactory.createBreakdown(true));
            r = api.createTaskFromText("do something", null);
            check("bad model output " + mode + " -> 502 and nothing saved", r.getStatus() == 502 && count() == 0);
        }

        api = newApi(ParserFactory.createParser(prompt -> {
            throw new LlmException("LLM provider returned HTTP 401");
        }), ParserFactory.createBreakdown(true));
        r = api.createTaskFromText("do something", null);
        check("LLM provider failure exposes safe diagnostic",
                r.getStatus() == 502 && String.valueOf(error(r).get("message")).contains("HTTP 401"));

        api = newApi(ParserFactory.createParser(false), ParserFactory.createBreakdown(false));
        if (!ParserFactory.isLlmConfigured()) {
            check("missing LLM_API_KEY -> 503 (no crash)", api.createTaskFromText("hello", null).getStatus() == 503);
        }

        api = newApi();
        r = api.createTaskFromText("Remind me to pay rent on the 1st of every month", null);
        check("rule-based parser: due 2026-12-01, monthly",
                r.getStatus() == 201 && "2026-12-01".equals(r.getBody().get("dueDate")) && "monthly".equals(r.getBody().get("recurrence")));
        r = api.createTaskFromText("Call the bank tomorrow", null);
        check("rule-based parser: 'tomorrow' -> 2026-11-26", "2026-11-26".equals(r.getBody().get("dueDate")));

        // ---------- Subtasks ----------
        api = newApi();
        api.createTask(req("Parent"));
        check("createSubtask -> 201 with parentId", api.createSubtask(1, req("Child")).getStatus() == 201);
        check("createSubtask unknown parent -> 404", api.createSubtask(99, req("x")).getStatus() == 404);
        check("subtask of a subtask -> 400", api.createSubtask(2, req("Grandchild")).getStatus() == 400);
        check("listSubtasks -> 1 item", api.listSubtasks(1).getBody().get("count").equals(1));
        check("listSubtasks unknown parent -> 404", api.listSubtasks(99).getStatus() == 404);
        check("break down a subtask -> 400", api.breakDownTask(2, null).getStatus() == 400);
        check("break down unknown task -> 404", api.breakDownTask(99, null).getStatus() == 404);
        api.createSubtask(1, req("Child 2"));
        check("delete parent removes subtasks", api.deleteTask(1).getStatus() == 200 && count() == 0);

        // ---------- Breakdown ----------
        FakeLlmClient subs = new FakeLlmClient(GOOD_SUBTASKS);
        api = newApi(ParserFactory.createParser(true), ParserFactory.createBreakdown(subs));
        api.createTask(req("Plan trip"));
        r = api.breakDownTask(1, "b1");
        check("LLM breakdown -> 201 with 3 subtasks", r.getStatus() == 201 && items(r).size() == 3 && count() == 4);
        api.breakDownTask(1, "b1");
        check("repeated breakdown key: fake called once, still 4 tasks", subs.getCalls() == 1 && count() == 4);

        api = newApi(ParserFactory.createParser(true), ParserFactory.createBreakdown(new FaultyLlmClient(Mode.MALFORMED)));
        api.createTask(req("Plan trip"));
        check("malformed breakdown -> 502, none saved", api.breakDownTask(1, null).getStatus() == 502 && count() == 1);

        api = newApi(ParserFactory.createParser(true), ParserFactory.createBreakdown(new FaultyLlmClient(Mode.THROWS)));
        api.createTask(req("Plan trip"));
        check("breakdown model failure -> 502, none saved", api.breakDownTask(1, null).getStatus() == 502 && count() == 1);

        api = newApi(ParserFactory.createParser(true), ParserFactory.createBreakdown(new FakeLlmClient("Good one|ok\n|missing title")));
        api.createTask(req("Plan trip"));
        check("one invalid item -> 502 and NONE saved (even the valid one)", api.breakDownTask(1, null).getStatus() == 502 && count() == 1);

        api = newApi();
        api.createTask(req("Plan trip, book flights and pack bags"));
        r = api.breakDownTask(1, null);
        check("rule-based breakdown splits on commas and 'and' -> 3 subtasks", r.getStatus() == 201 && items(r).size() == 3);
        api.createTask(req("Single"));
        check("rule-based breakdown of unsplittable title -> 400", api.breakDownTask(5, null).getStatus() == 400);

        // ---------- Query ----------
        api = newApi();
        api.createTask(new TaskRequest("Buy milk", "dairy", "false", null, "LOW"));
        api.createTask(new TaskRequest("Buy bread", "bakery", "false", "2026-12-01", "HIGH"));
        api.createTask(new TaskRequest("Pay rent", "monthly", "true", null, "HIGH"));
        api.createTask(new TaskRequest("Buy eggs", "dozen", "false", null, "MEDIUM"));
        TaskQuery q = new TaskQuery.Builder().search("buy").completed(false).sortBy(new SortByPriority()).pageSize(2).page(1).build();
        r = api.listTasks(q);
        check("filter+search+sort+page together: page 1 = bread, eggs",
                items(r).size() == 2 && "Buy bread".equals(items(r).get(0).get("title")) && "Buy eggs".equals(items(r).get(1).get("title")));
        check("totalItems 3, totalPages 2", r.getBody().get("totalItems").equals(3) && r.getBody().get("totalPages").equals(2));
        r = api.listTasks(new TaskQuery.Builder().search("buy").completed(false).sortBy(new SortByPriority()).pageSize(2).page(2).build());
        check("page 2 = milk", items(r).size() == 1 && "Buy milk".equals(items(r).get(0).get("title")));
        r = api.listTasks(new TaskQuery.Builder().pageSize(2).page(99).build());
        check("page beyond last -> 200 with empty items", r.getStatus() == 200 && items(r).isEmpty());
        check("page 0 -> 400", api.listTasks(new TaskQuery.Builder().page(0).build()).getStatus() == 400);
        check("pageSize 1000 -> 400", api.listTasks(new TaskQuery.Builder().pageSize(1000).build()).getStatus() == 400);
        r = api.listTasks(new TaskQuery.Builder().priority(Priority.HIGH).build());
        check("priority filter -> 2 HIGH tasks", items(r).size() == 2);
        r = api.listTasks(new TaskQuery.Builder().dueBefore(LocalDate.of(2026, 12, 31)).build());
        check("dueBefore filter ignores tasks with no date", items(r).size() == 1);
        r = api.listTasks(new TaskQuery.Builder().sortBy(new query.SortByTitle()).descending(true).build());
        check("sort by title descending starts with 'Pay rent'", "Pay rent".equals(items(r).get(0).get("title")));
        check("null query -> 200 (defaults)", api.listTasks(null).getStatus() == 200);

        // ---------- Observer ----------
        api = newApi();
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(audit);
        api.createTask(req("a"));
        api.createTask(req("b"));
        api.updateTask(1, req("a2"));
        api.completeTask(1);
        api.deleteTask(2);
        check("audit counts: 2 created, 1 updated, 1 completed, 1 deleted",
                audit.getCount(TaskEventListener.CREATED) == 2 && audit.getCount(TaskEventListener.UPDATED) == 1
                        && audit.getCount(TaskEventListener.COMPLETED) == 1 && audit.getCount(TaskEventListener.DELETED) == 1);
        service.removeListener(audit);
        api.createTask(req("c"));
        check("removed listener gets no more events", audit.getCount(TaskEventListener.CREATED) == 2);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
