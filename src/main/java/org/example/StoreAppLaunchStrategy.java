package org.example;

/** Launches Microsoft Store applications by AUMID through shell:AppsFolder. */
public final class StoreAppLaunchStrategy implements LaunchStrategy {
    private final WindowsShellExecutor shellExecutor;

    public StoreAppLaunchStrategy(WindowsShellExecutor shellExecutor) {
        this.shellExecutor = shellExecutor == null
                ? new WindowsShellExecutor()
                : shellExecutor;
    }

    @Override
    public LaunchResult launch(AppShortcut shortcut) {
        String target = normalizeStoreTarget(shortcut.getTarget());
        return shellExecutor.open(
                target,
                null,
                null,
                "Запущено Store-приложение: " + shortcut.getDisplayName()
        );
    }

    private String normalizeStoreTarget(String rawTarget) {
        String value = LaunchPathResolver.normalize(rawTarget);
        if (value.regionMatches(true, 0, "shell:AppsFolder", 0, 16)) {
            return value.replace('/', '\\');
        }
        return "shell:AppsFolder\\" + value;
    }
}
