package repository;

import model.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Singleton in-memory store. */
public class InMemoryTaskRepository implements TaskRepository {
    private static final InMemoryTaskRepository INSTANCE = new InMemoryTaskRepository();

    private final Map<Integer, Task> store = new LinkedHashMap<>();
    private int nextId = 1;

    private InMemoryTaskRepository() {}

    public static InMemoryTaskRepository getInstance() { return INSTANCE; }

    @Override
    public synchronized Task save(Task task) {
        Task toStore = task;
        if (task.getId() == 0) {
            toStore = task.toBuilder().id(nextId++).build();
        }
        store.put(toStore.getId(), toStore);
        return toStore;
    }

    @Override
    public synchronized Optional<Task> findById(int id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public synchronized List<Task> findAll() { return new ArrayList<>(store.values()); }

    @Override
    public synchronized boolean delete(int id) { return store.remove(id) != null; }

    /** For demos and tests: empties the store and restarts ids at 1. */
    public synchronized void clear() {
        store.clear();
        nextId = 1;
    }
}
