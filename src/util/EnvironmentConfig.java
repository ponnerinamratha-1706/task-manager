package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnvironmentConfig {
    private EnvironmentConfig() {}

    public static String get(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) return value;

        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) return null;

        try {
            for (String line : Files.readAllLines(envFile)) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) continue;
                if (entry.startsWith("export ")) entry = entry.substring(7).trim();

                int separator = entry.indexOf('=');
                if (separator < 0 || !entry.substring(0, separator).trim().equals(name)) continue;

                value = entry.substring(separator + 1).trim();
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                return value.isBlank() ? null : value;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read .env", e);
        }
        return null;
    }
}
