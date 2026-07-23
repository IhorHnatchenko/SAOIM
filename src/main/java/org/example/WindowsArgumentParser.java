package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a user-entered Windows argument string without using cmd.exe.
 * Supports quoted arguments and the standard backslash-before-quote rules.
 */
public final class WindowsArgumentParser {
    public List<String> parse(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < commandLine.length()) {
            while (index < commandLine.length()
                    && Character.isWhitespace(commandLine.charAt(index))) {
                index++;
            }
            if (index >= commandLine.length()) {
                break;
            }

            StringBuilder argument = new StringBuilder();
            boolean quoted = false;
            while (index < commandLine.length()) {
                char current = commandLine.charAt(index);
                if (!quoted && Character.isWhitespace(current)) {
                    break;
                }

                if (current == '\\') {
                    int slashStart = index;
                    while (index < commandLine.length()
                            && commandLine.charAt(index) == '\\') {
                        index++;
                    }
                    int slashCount = index - slashStart;
                    if (index < commandLine.length() && commandLine.charAt(index) == '"') {
                        argument.append("\\".repeat(slashCount / 2));
                        if (slashCount % 2 == 0) {
                            quoted = !quoted;
                        } else {
                            argument.append('"');
                        }
                        index++;
                    } else {
                        argument.append("\\".repeat(slashCount));
                    }
                    continue;
                }

                if (current == '"') {
                    if (quoted && index + 1 < commandLine.length()
                            && commandLine.charAt(index + 1) == '"') {
                        argument.append('"');
                        index += 2;
                    } else {
                        quoted = !quoted;
                        index++;
                    }
                    continue;
                }

                argument.append(current);
                index++;
            }

            if (quoted) {
                throw new IllegalArgumentException("В аргументах не закрыта кавычка.");
            }
            result.add(argument.toString());
            while (index < commandLine.length()
                    && Character.isWhitespace(commandLine.charAt(index))) {
                index++;
            }
        }
        return List.copyOf(result);
    }
}
