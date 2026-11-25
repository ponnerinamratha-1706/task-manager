package util;

import java.util.Map;

/** Tiny JSON writer for Map / List / String / Number / Boolean / enum / null. */
public final class JsonWriter {
    private JsonWriter() {}

    public static String write(Object value, boolean pretty) {
        StringBuilder sb = new StringBuilder();
        write(value, pretty, 0, sb);
        return sb.toString();
    }

    private static void write(Object v, boolean pretty, int level, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            quote(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Enum<?> e) {
            quote(e.name(), sb);
        } else if (v instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                newline(pretty, level + 1, sb);
                quote(String.valueOf(entry.getKey()), sb);
                sb.append(pretty ? ": " : ":");
                write(entry.getValue(), pretty, level + 1, sb);
            }
            newline(pretty, level, sb);
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            if (!it.iterator().hasNext()) { sb.append("[]"); return; }
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                newline(pretty, level + 1, sb);
                write(o, pretty, level + 1, sb);
            }
            newline(pretty, level, sb);
            sb.append(']');
        } else {
            quote(v.toString(), sb);
        }
    }

    private static void newline(boolean pretty, int level, StringBuilder sb) {
        if (!pretty) return;
        sb.append('\n');
        for (int i = 0; i < level; i++) sb.append("  ");
    }

    private static void quote(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
