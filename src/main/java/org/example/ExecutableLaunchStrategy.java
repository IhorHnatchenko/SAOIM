package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Launches .exe files directly with ProcessBuilder and no intermediate shell. */
public final class ExecutableLaunchStrategy implements LaunchStrategy {
    private final WindowsArgumentParser argumentParser;

    public ExecutableLaunchStrategy(WindowsArgumentParser argumentParser) {
        this.argumentParser = argumentParser == null
                ? new WindowsArgumentParser()
                : argumentParser;
    }

    @Override
    public LaunchResult launch(AppShortcut shortcut) throws Exception {
        Path executable = LaunchPathResolver.resolvePath(shortcut.getTarget());
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(argumentParser.parse(shortcut.getArguments()));

        ProcessBuilder builder = new ProcessBuilder(command);
        Path workingDirectory = resolveWorkingDirectory(shortcut, executable);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.start();
        return LaunchResult.success("Запущено: " + shortcut.getDisplayName());
    }

    private Path resolveWorkingDirectory(AppShortcut shortcut, Path executable) {
        if (shortcut.getWorkingDirectory() != null
                && !shortcut.getWorkingDirectory().isBlank()) {
            Path directory = LaunchPathResolver.resolvePath(shortcut.getWorkingDirectory());
            if (!Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Рабочая папка не найдена: " + directory);
            }
            return directory;
        }
        return executable.getParent();
    }
}
