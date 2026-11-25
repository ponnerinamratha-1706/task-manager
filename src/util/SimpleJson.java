package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader. Objects become Map, arrays become List, numbers become Double,
 * plus String, Boolean and null. Throws IllegalArgumentException on malformed input.
 */
public final class SimpleJson {
    private final String s;
    private int i = 0;

    private SimpleJson(String s) { this.s = s; }

    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("null input");
        SimpleJson p = new SimpleJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.error("unexpected trailing characters");
        return v;
    }

    /** Finds the first {...} block (models sometimes wrap JSON in ``` fences) and parses it. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        if (text == null) throw new IllegalArgumentException("null input");
        int a = text.indexOf('{');
        int b = text.lastIndexOf('}');
        if (a < 0 || b < a) throw new IllegalArgumentException("no JSON object found");
        Object v = parse(text.substring(a, b + 1));
        if (!(v instanceof Map)) throw new IllegalArgumentException("not a JSON object");
        return (Map<String, Object>) v;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("Invalid JSON at position " + i + ": " + msg);
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private Object value() {
        if (i >= s.length()) throw error("unexpected end");
        char c = s.charAt(i);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') throw error("expected string key");
            String key = string();
            ws();
            if (i >= s.length() || s.charAt(i) != ':') throw error("expected ':'");
            i++; ws();
            m.put(key, value());
            ws();
            if (i >= s.length()) throw error("unexpected end");
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw error("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++; ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; return list; }
        while (true) {
            ws();
            list.add(value());
            ws();
            if (i >= s.length()) throw error("unexpected end");
            char c = s.charAt(i++);
            if (c == ']') return list;
            if (c != ',') throw error("expected ',' or ']'");
        }
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        i++; // opening quote
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error("bad unicode escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> sb.append(e); // \" \\ \/
                }
            } else {
                sb.append(c);
            }
        }
        throw error("unterminated string");
    }

    private Double number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw error("unexpected character '" + s.charAt(i) + "'");
        try {
            return Double.parseDouble(s.substring(start, i));
        } catch (NumberFormatException e) {
            throw error("bad number");
        }
    }
}
