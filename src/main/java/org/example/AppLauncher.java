package org.example;

import java.util.EnumMap;
import java.util.Map;

/** Facade that validates a shortcut and delegates it to the correct strategy. */
public final class AppLauncher {
    private final ShortcutAvailabilityService availabilityService;
    private final Map<LaunchType, LaunchStrategy> strategies;

    public AppLauncher() {
        this(new ShortcutAvailabilityService());
    }

    public AppLauncher(ShortcutAvailabilityService availabilityService) {
        this.availabilityService = availabilityService == null
                ? new ShortcutAvailabilityService()
                : availabilityService;

        WindowsShellExecutor shell = new WindowsShellExecutor();
        LaunchStrategy executable = new ExecutableLaunchStrategy(
                new WindowsArgumentParser()
        );
        LaunchStrategy shellOpen = new ShellLaunchStrategy(shell);

        EnumMap<LaunchType, LaunchStrategy> configured = new EnumMap<>(LaunchType.class);
        configured.put(LaunchType.EXECUTABLE, executable);
        configured.put(LaunchType.WINDOWS_SHORTCUT, shellOpen);
        configured.put(LaunchType.FILE, shellOpen);
        configured.put(LaunchType.DIRECTORY, shellOpen);
        configured.put(LaunchType.URL, new UrlLaunchStrategy(shell));
        configured.put(LaunchType.MICROSOFT_STORE_APP, new StoreAppLaunchStrategy(shell));
        strategies = Map.copyOf(configured);
    }

    public LaunchAvailability checkAvailability(AppShortcut shortcut) {
        return availabilityService.check(shortcut);
    }

    public ShortcutAvailabilityService getAvailabilityService() {
        return availabilityService;
    }

    public LaunchResult launch(AppShortcut shortcut) {
        LaunchAvailability availability = availabilityService.check(shortcut);
        if (!availability.available()) {
            return LaunchResult.failure(availability.message());
        }

        LaunchStrategy strategy = strategies.get(shortcut.getLaunchType());
        if (strategy == null) {
            return LaunchResult.failure(
                    "Для типа " + shortcut.getLaunchType() + " не настроена стратегия запуска."
            );
        }

        try {
            LaunchResult result = strategy.launch(shortcut);
            return result == null
                    ? LaunchResult.failure("Стратегия запуска не вернула результат.")
                    : result;
        } catch (Throwable throwable) {
            return LaunchResult.failure(
                    "Не удалось запустить «" + shortcut.getDisplayName() + "»: "
                            + safeMessage(throwable),
                    throwable
            );
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }
}
