package org.example;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands common Windows environment variables without invoking a command shell. */
public final class LaunchPathResolver {
    private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("%([^%]+)%");

    private LaunchPathResolver() {
    }

    public static Path resolvePath(String rawValue) {
        String value = normalize(rawValue);
        if (value.startsWith("~\\") || value.startsWith("~/")) {
            value = System.getProperty("user.home", "") + value.substring(1);
        }
        value = expandEnvironmentVariables(value);
        return Path.of(value).toAbsolutePath().normalize();
    }

    public static String normalize(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    public static String expandEnvironmentVariables(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Map<String, String> environment = System.getenv();
        Matcher matcher = ENVIRONMENT_VARIABLE.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = environment.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(matcher.group(1)))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
