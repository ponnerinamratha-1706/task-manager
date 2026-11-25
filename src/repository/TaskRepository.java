package repository;

import model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    /** Saves a task. If its id is 0, a new id is assigned. Returns the stored task. */
    Task save(Task task);
    Optional<Task> findById(int id);
    List<Task> findAll();
    boolean delete(int id);
}
