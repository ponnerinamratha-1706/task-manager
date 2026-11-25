package query;

import model.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PageResult {
    private final List<Task> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    public PageResult(List<Task> items, int page, int pageSize, int totalItems, int totalPages) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<Task> getItems() { return items; }
    public int getTotalItems() { return totalItems; }
    public int getTotalPages() { return totalPages; }

    public Map<String, Object> toMap() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Task t : items) list.add(t.toMap());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", list);
        m.put("page", page);
        m.put("pageSize", pageSize);
        m.put("totalItems", totalItems);
        m.put("totalPages", totalPages);
        return m;
    }
}
