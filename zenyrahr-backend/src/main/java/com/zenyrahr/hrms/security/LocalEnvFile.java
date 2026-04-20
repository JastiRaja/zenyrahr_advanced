package com.zenyrahr.hrms.security;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class LocalEnvFile {
    private static final Map<String, String> ENV_FILE_VALUES = loadEnvFile();

    private LocalEnvFile() {}

    public static String get(String key) {
        return ENV_FILE_VALUES.get(key);
    }

    public static boolean hasText(String key) {
        String value = ENV_FILE_VALUES.get(key);
        return value != null && !value.trim().isEmpty();
    }

    private static Map<String, String> loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int separator = line.indexOf('=');
                if (separator <= 0) continue;

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException ex) {
            log.warn("Unable to read .env file at {}", envPath.toAbsolutePath(), ex);
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(values);
    }
}
