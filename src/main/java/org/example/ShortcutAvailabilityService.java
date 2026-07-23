package org.example;

import javafx.application.Platform;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/** Fast local validation used both by the launcher and orbit presentation. */
public final class ShortcutAvailabilityService {
    private static final ExecutorService AVAILABILITY_EXECUTOR = Executors.newFixedThreadPool(
            2,
            new AvailabilityThreadFactory()
    );

    /**
     * Performs path and shell-target checks outside JavaFX Application Thread.
     * The callback is always delivered on the JavaFX thread when the toolkit is running.
     */
    public CompletableFuture<LaunchAvailability> checkAsync(
            AppShortcut shortcut,
            Consumer<LaunchAvailability> onCompleted
    ) {
        CompletableFuture<LaunchAvailability> future = CompletableFuture.supplyAsync(
                () -> check(shortcut),
                AVAILABILITY_EXECUTOR
        );
        if (onCompleted != null) {
            future.whenComplete((availability, error) -> {
                LaunchAvailability result = error == null
                        ? availability
                        : LaunchAvailability.unavailable(
                                "Не удалось проверить ярлык: " + safeMessage(error)
                        );
                runOnJavaFxThread(() -> onCompleted.accept(result));
            });
        }
        return future;
    }

    public LaunchAvailability check(AppShortcut shortcut) {
        if (shortcut == null) {
            return LaunchAvailability.unavailable("Данные ярлыка отсутствуют.");
        }
        if (!shortcut.isEnabled()) {
            return LaunchAvailability.unavailable("Ярлык отключён пользователем.");
        }
        if (shortcut.getTarget() == null || shortcut.getTarget().isBlank()) {
            return LaunchAvailability.unavailable("Не указана цель запуска.");
        }

        try {
            return switch (shortcut.getLaunchType()) {
                case EXECUTABLE -> checkExecutable(shortcut);
                case WINDOWS_SHORTCUT -> checkWindowsShortcut(shortcut);
                case FILE -> checkFile(shortcut);
                case DIRECTORY -> checkDirectory(shortcut);
                case URL -> checkUrl(shortcut);
                case MICROSOFT_STORE_APP -> checkStoreApp(shortcut);
            };
        } catch (RuntimeException exception) {
            return LaunchAvailability.unavailable(
                    "Некорректная цель запуска: " + safeMessage(exception)
            );
        }
    }

    private LaunchAvailability checkExecutable(AppShortcut shortcut) {
        Path target = LaunchPathResolver.resolvePath(shortcut.getTarget());
        if (!Files.isRegularFile(target)) {
            return LaunchAvailability.unavailable("EXE-файл не найден: " + target);
        }
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe")) {
            return LaunchAvailability.unavailable("Для типа EXE требуется файл с расширением .exe.");
        }
        LaunchAvailability workDir = checkWorkingDirectory(shortcut);
        return workDir.available()
                ? LaunchAvailability.available("EXE-файл доступен")
                : workDir;
    }

    private LaunchAvailability checkWindowsShortcut(AppShortcut shortcut) {
        Path target = LaunchPathResolver.resolvePath(shortcut.getTarget());
        if (!Files.isRegularFile(target)) {
            return LaunchAvailability.unavailable("Ярлык Windows не найден: " + target);
        }
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lnk")) {
            return LaunchAvailability.unavailable("Для этого типа требуется файл .lnk.");
        }
        return LaunchAvailability.available("Ярлык Windows доступен");
    }

    private LaunchAvailability checkFile(AppShortcut shortcut) {
        Path target = LaunchPathResolver.resolvePath(shortcut.getTarget());
        return Files.isRegularFile(target)
                ? LaunchAvailability.available("Файл доступен")
                : LaunchAvailability.unavailable("Файл не найден: " + target);
    }

    private LaunchAvailability checkDirectory(AppShortcut shortcut) {
        Path target = LaunchPathResolver.resolvePath(shortcut.getTarget());
        return Files.isDirectory(target)
                ? LaunchAvailability.available("Папка доступна")
                : LaunchAvailability.unavailable("Папка не найдена: " + target);
    }

    private LaunchAvailability checkUrl(AppShortcut shortcut) {
        URI uri = URI.create(shortcut.getTarget().trim());
        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return LaunchAvailability.unavailable("URL должен начинаться с http:// или https://.");
        }
        return LaunchAvailability.available("URL корректен");
    }

    private LaunchAvailability checkStoreApp(AppShortcut shortcut) {
        if (!isWindows()) {
            return LaunchAvailability.unavailable("Microsoft Store поддерживается только в Windows.");
        }
        String target = shortcut.getTarget().trim();
        boolean shellPath = target.regionMatches(true, 0, "shell:AppsFolder", 0, 16);
        boolean aumid = target.contains("!");
        if (!shellPath && !aumid) {
            return LaunchAvailability.unavailable(
                    "Укажите AUMID вида PackageFamilyName!App или shell:AppsFolder\\AUMID."
            );
        }
        return LaunchAvailability.available(
                "Формат AUMID принят; наличие приложения проверит Windows при запуске"
        );
    }

    private LaunchAvailability checkWorkingDirectory(AppShortcut shortcut) {
        if (shortcut.getWorkingDirectory() == null || shortcut.getWorkingDirectory().isBlank()) {
            return LaunchAvailability.available("");
        }
        Path directory = LaunchPathResolver.resolvePath(shortcut.getWorkingDirectory());
        return Files.isDirectory(directory)
                ? LaunchAvailability.available("")
                : LaunchAvailability.unavailable("Рабочая папка не найдена: " + directory);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private void runOnJavaFxThread(Runnable runnable) {
        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                Platform.runLater(runnable);
            }
        } catch (IllegalStateException exception) {
            runnable.run();
        }
    }

    private static final class AvailabilityThreadFactory implements ThreadFactory {
        private int counter;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                    runnable,
                    "saoim-shortcut-check-" + (++counter)
            );
            thread.setDaemon(true);
            return thread;
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }
}
