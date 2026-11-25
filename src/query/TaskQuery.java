package query;

import model.Priority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Everything is optional, so it is built with the Builder pattern. */
public class TaskQuery {
    private final List<TaskFilter> filters;
    private final SortStrategy sort;
    private final boolean descending;
    private final int page;
    private final int pageSize;

    private TaskQuery(Builder b) {
        this.filters = List.copyOf(b.filters);
        this.sort = b.sort;
        this.descending = b.descending;
        this.page = b.page;
        this.pageSize = b.pageSize;
    }

    public List<TaskFilter> getFilters() { return filters; }
    public SortStrategy getSort() { return sort; }
    public boolean isDescending() { return descending; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    public static class Builder {
        private final List<TaskFilter> filters = new ArrayList<>();
        private SortStrategy sort = new SortById();
        private boolean descending = false;
        private int page = 1;
        private int pageSize = 10;

        public Builder filter(TaskFilter f) { filters.add(f); return this; }
        public Builder completed(boolean completed) { return filter(new CompletedFilter(completed)); }
        public Builder priority(Priority p) { return filter(new PriorityFilter(p)); }
        public Builder dueBefore(LocalDate d) { return filter(new DueBeforeFilter(d)); }
        public Builder search(String text) { return filter(new SearchFilter(text)); }
        public Builder sortBy(SortStrategy s) { this.sort = s; return this; }
        public Builder descending(boolean d) { this.descending = d; return this; }
        public Builder page(int page) { this.page = page; return this; }
        public Builder pageSize(int pageSize) { this.pageSize = pageSize; return this; }
        public TaskQuery build() { return new TaskQuery(this); }
    }
}
