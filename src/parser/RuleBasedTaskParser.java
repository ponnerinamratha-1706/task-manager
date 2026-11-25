package parser;

import java.time.LocalDate;
import java.util.Locale;

/** Offline parser built with simple String methods. */
public class RuleBasedTaskParser implements TaskParser {
    @Override
    public ParsedTask parse(String text, String today) {
        String t = text.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        LocalDate base = LocalDate.parse(today);

        String due = null;
        if (lower.contains("tomorrow")) {
            due = base.plusDays(1).toString();
        } else if (lower.contains("today")) {
            due = base.toString();
        } else if (lower.contains("1st")) {
            LocalDate first = base.getDayOfMonth() == 1 ? base : base.withDayOfMonth(1).plusMonths(1);
            due = first.toString();
        }

        String recurrence = null;
        if (lower.contains("every month") || lower.contains("monthly")) recurrence = "monthly";
        else if (lower.contains("every week") || lower.contains("weekly")) recurrence = "weekly";
        else if (lower.contains("every day") || lower.contains("daily")) recurrence = "daily";

        String title = t;
        if (lower.startsWith("remind me to ")) title = t.substring("remind me to ".length()).trim();
        if (!title.isEmpty()) title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

        return new ParsedTask(title, t, due, recurrence);
    }
}
