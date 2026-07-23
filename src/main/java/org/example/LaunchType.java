package org.example;

/** Supported shortcut target types. Actual launching is implemented at step 8. */
public enum LaunchType {
    EXECUTABLE("EXE-приложение", "▣"),
    WINDOWS_SHORTCUT("Ярлык Windows (.lnk)", "↗"),
    FILE("Файл", "▤"),
    DIRECTORY("Папка", "▰"),
    URL("URL", "◎"),
    MICROSOFT_STORE_APP("Microsoft Store", "▦");

    private final String displayName;
    private final String defaultIcon;

    LaunchType(String displayName, String defaultIcon) {
        this.displayName = displayName;
        this.defaultIcon = defaultIcon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultIcon() {
        return defaultIcon;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
