package event;

import model.Task;

public interface TaskEventListener {
    String CREATED = "CREATED";
    String UPDATED = "UPDATED";
    String COMPLETED = "COMPLETED";
    String DELETED = "DELETED";

    void onEvent(String eventType, Task task);
}
