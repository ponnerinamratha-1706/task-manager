package parser;

import exception.ValidationException;
import model.Task;
import model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

/** Offline strategy: splits the task title on commas and the word "and". */
public class RuleBasedBreakdownStrategy implements BreakdownStrategy {
    @Override
    public List<TaskRequest> breakDown(Task task) {
        String[] parts = task.getTitle().split(",|\\band\\b");
        List<TaskRequest> result = new ArrayList<>();
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            result.add(new TaskRequest(p, "Part of: " + task.getTitle(), "false", null, null));
        }
        if (result.size() < 2) {
            throw new ValidationException("title", "cannot be split into subtasks (use commas or \"and\")");
        }
        return result;
    }
}
