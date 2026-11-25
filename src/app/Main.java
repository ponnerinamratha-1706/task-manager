package app;

import api.ApiResponse;
import api.IdempotencyStore;
import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import model.TaskRequest;
import parser.ParserFactory;
import query.TaskQuery;
import repository.InMemoryTaskRepository;
import service.TaskService;

import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    static void show(String label, ApiResponse response) {
        System.out.println("\n--- " + label + " -> HTTP " + response.getStatus());
        System.out.println(response.toJson());
    }

    public static void main(String[] args) {
        System.out.println("Task Manager started");

        boolean offline = !ParserFactory.isLlmConfigured();
        System.out.println(offline
                ? "(no LLM_API_KEY: using rule-based parsers)"
                : "(LLM_API_KEY found: using the LLM)");

        InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();
        repo.clear();
        IdempotencyStore.getInstance().clear();

        TaskService service = new TaskService(repo,
                ParserFactory.createParser(offline),
                ParserFactory.createBreakdown(offline),
                LocalDate::now);
        TaskApi api = new TaskApi(service, IdempotencyStore.getInstance());

        ConsoleLogListener log = new ConsoleLogListener();
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(log);
        service.addListener(audit);

        try (Scanner input = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = readLine(input, "Choose an option: ");
                if (choice == null || choice.trim().equals("0")) break;

                switch (choice.trim()) {
                    case "1":
                        createTask(input, api);
                        break;
                    case "2":
                        createTaskFromText(input, api);
                        break;
                    case "3":
                        show("listTasks", api.listTasks(new TaskQuery.Builder().build()));
                        break;
                    case "4":
                        getTask(input, api);
                        break;
                    case "5":
                        updateTask(input, api);
                        break;
                    case "6":
                        completeTask(input, api);
                        break;
                    case "7":
                        deleteTask(input, api);
                        break;
                    case "8":
                        createSubtask(input, api);
                        break;
                    case "9":
                        listSubtasks(input, api);
                        break;
                    case "10":
                        breakDownTask(input, api);
                        break;
                    default:
                        System.out.println("Please choose an option from 0 to 10.");
                }
            }
        }

        System.out.println("Audit trail: " + audit.summary());
        System.out.println("Task Manager stopped");
    }

    private static void printMenu() {
        System.out.println("\n--- Task Manager ---");
        System.out.println("1. Create task");
        System.out.println("2. Create task from text (LLM/rule-based)");
        System.out.println("3. List tasks");
        System.out.println("4. Get task");
        System.out.println("5. Update task");
        System.out.println("6. Complete task");
        System.out.println("7. Delete task");
        System.out.println("8. Create subtask");
        System.out.println("9. List subtasks");
        System.out.println("10. Break down task");
        System.out.println("0. Exit");
    }

    private static void createTask(Scanner input, TaskApi api) {
        TaskRequest request = readTaskRequest(input);
        if (request == null) return;
        String key = readOptional(input, "Idempotency key (optional): ");
        show("createTask", api.createTask(request, key));
    }

    private static void createTaskFromText(Scanner input, TaskApi api) {
        String text = readLine(input, "Describe the task: ");
        if (text == null) return;
        String key = readOptional(input, "Idempotency key (optional): ");
        show("createTaskFromText", api.createTaskFromText(text, key));
    }

    private static void getTask(Scanner input, TaskApi api) {
        Integer id = readId(input, "Task ID: ");
        if (id != null) show("getTask", api.getTask(id));
    }

    private static void updateTask(Scanner input, TaskApi api) {
        Integer id = readId(input, "Task ID to update: ");
        if (id == null) return;
        TaskRequest request = readTaskRequest(input);
        if (request != null) show("updateTask", api.updateTask(id, request));
    }

    private static void completeTask(Scanner input, TaskApi api) {
        Integer id = readId(input, "Task ID to complete: ");
        if (id != null) show("completeTask", api.completeTask(id));
    }

    private static void deleteTask(Scanner input, TaskApi api) {
        Integer id = readId(input, "Task ID to delete: ");
        if (id != null) show("deleteTask", api.deleteTask(id));
    }

    private static void createSubtask(Scanner input, TaskApi api) {
        Integer parentId = readId(input, "Parent task ID: ");
        if (parentId == null) return;
        TaskRequest request = readTaskRequest(input);
        if (request != null) show("createSubtask", api.createSubtask(parentId, request));
    }

    private static void listSubtasks(Scanner input, TaskApi api) {
        Integer parentId = readId(input, "Parent task ID: ");
        if (parentId != null) show("listSubtasks", api.listSubtasks(parentId));
    }

    private static void breakDownTask(Scanner input, TaskApi api) {
        Integer id = readId(input, "Task ID to break down: ");
        if (id == null) return;
        String key = readOptional(input, "Idempotency key (optional): ");
        show("breakDownTask", api.breakDownTask(id, key));
    }

    private static TaskRequest readTaskRequest(Scanner input) {
        System.out.println("Enter task details (leave completed blank for false; date and priority are optional).");
        String title = readLine(input, "Title: ");
        if (title == null) return null;
        String description = readLine(input, "Description: ");
        if (description == null) return null;
        String completed = readLine(input, "Completed (true/false) [false]: ");
        if (completed == null) return null;
        String dueDate = readLine(input, "Due date (YYYY-MM-DD, optional): ");
        if (dueDate == null) return null;
        String priority = readLine(input, "Priority (LOW/MEDIUM/HIGH, optional): ");
        if (priority == null) return null;

        return new TaskRequest(title, description,
                completed.isBlank() ? "false" : completed,
                dueDate.isBlank() ? null : dueDate,
                priority.isBlank() ? null : priority);
    }

    private static Integer readId(Scanner input, String prompt) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) return null;
            try {
                return Integer.valueOf(value.trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid numeric ID.");
            }
        }
    }

    private static String readOptional(Scanner input, String prompt) {
        String value = readLine(input, prompt);
        return value == null || value.isBlank() ? null : value;
    }

    private static String readLine(Scanner input, String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine() : null;
    }
}
