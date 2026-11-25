# Task Manager (plain Java 17)

A task manager that behaves like a REST API without a web framework. Each operation is a method on `TaskApi`
that returns an `ApiResponse` (status code + JSON-style body). An LLM turns plain English such as
"Remind me to pay rent on the 1st of every month" into a structured task and can break big tasks into subtasks.

## Package structure
| Package | Contents |
|---|---|
| `model` | `Task` (immutable, Builder), `TaskRequest`, `Priority` |
| `repository` | `TaskRepository`, `InMemoryTaskRepository` (Singleton) |
| `service` | `TaskService` (business logic, Observer subject), `TaskValidator` |
| `parser` | `TaskParser`, `LlmTaskParser`, `RuleBasedTaskParser`, `BreakdownStrategy` + 2 implementations, `ParserFactory` |
| `query` | `TaskQuery` (Builder), `TaskFilter` + filters, `SortStrategy` + sorts, `PageResult` |
| `event` | `TaskEventListener`, `ConsoleLogListener`, `AuditTrailListener` |
| `exception` | `ApiException` hierarchy, `FieldIssue` |
| `api` | `TaskApi`, `ApiResponse`, `ErrorResponseFactory`, `IdempotencyStore` (Singleton) |
| `llm` | `LlmClient`, `HttpLlmClient`, `FakeLlmClient`, `FaultyLlmClient` |
| `util` | `JsonWriter`, `SimpleJson` |
| `app` | `Main`, `TestRunner` |

## Setup
- JDK 17 or newer. No external libraries.
- Compile: `javac -d out $(find src -name '*.java')` (Windows PowerShell: `javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`)
- Run: `java -cp out app.Main` to open the interactive menu for creating, viewing, updating,
  completing, deleting, and breaking down tasks. Tests: `java -cp out app.TestRunner` (no key or internet needed).
- LLM key (optional): set `LLM_API_KEY` in the root `.env` file or in your process environment
  (macOS/Linux: `export LLM_API_KEY=your-key`; Windows PowerShell: `$env:LLM_API_KEY="your-key"`).
  A process environment variable takes precedence over `.env`. Without a key, `Main` uses the rule-based
  parsers, and an LLM-mode call answers 503. Never commit a key.

## TaskApi operations
| Operation | Parameters | Success | Errors |
|---|---|---|---|
| `createTask` | `TaskRequest`, `idempotencyKey` (nullable) | 201 | 400, 422, 500 |
| `createTaskFromText` | `text`, `idempotencyKey` | 201 | 400, 422, 502, 503 |
| `getTask` | `id` | 200 | 404 |
| `listTasks` | `TaskQuery` | 200 | 400 |
| `updateTask` | `id`, `TaskRequest` | 200 | 400, 404 |
| `completeTask` | `id` | 200 | 404 |
| `deleteTask` (also deletes subtasks) | `id` | 200 | 404 |
| `createSubtask` | `parentId`, `TaskRequest` | 201 | 400, 404 |
| `listSubtasks` | `parentId` | 200 | 404 |
| `breakDownTask` | `id`, `idempotencyKey` | 201 | 400, 404, 422, 502, 503 |

TODO: for each operation, paste an example call and the printed response from `Main` (include an AI creation,
a breakdown and a validation error).

## Plugging in another parser
Implement `TaskParser` (`ParsedTask parse(String text, String today)`), then pass it to the `TaskService`
constructor or add it to `ParserFactory`. Throw `ApiException` subclasses (for example `BadGatewayException`) on failure.
`BreakdownStrategy` works the same way.

## Design patterns
| Pattern | Where |
|---|---|
| Builder | `Task.Builder`, `TaskQuery.Builder` |
| Singleton | `InMemoryTaskRepository`, `IdempotencyStore` |
| Factory | `ErrorResponseFactory`, `ParserFactory` |
| Strategy | `TaskParser`, `BreakdownStrategy`, `SortStrategy` (and `TaskFilter`) |
| Observer | `TaskService` + `TaskEventListener` |

## Prompts and why
TODO: paste the two prompts from `LlmTaskParser.buildPrompt` and `LlmBreakdownStrategy`, and explain: today's date is
included so relative dates resolve; "ONLY JSON" and a fixed key list make parsing predictable; the user text sits between
triple quotes and is declared to be data (reduces prompt injection); one `title|description` per line is easy to parse with String methods.

## What the AI-generated tests missed
TODO: write this yourself after you run your own AI-generated tests and compare with `TestRunner`.
