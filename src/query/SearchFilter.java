package query;

import model.Task;

import java.util.Locale;

/** Case-insensitive search in title and description. */
public class SearchFilter implements TaskFilter {
    private final String text;
    public SearchFilter(String text) { this.text = text.toLowerCase(Locale.ROOT); }
    @Override public boolean matches(Task t) {
        return t.getTitle().toLowerCase(Locale.ROOT).contains(text)
                || t.getDescription().toLowerCase(Locale.ROOT).contains(text);
    }
}
