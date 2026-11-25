package parser;

import model.Task;
import model.TaskRequest;

import java.util.List;

/** Strategy interface: proposes subtasks for a task. Output is untrusted until validated. */
public interface BreakdownStrategy {
    List<TaskRequest> breakDown(Task task);
}
