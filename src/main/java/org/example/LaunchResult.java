package org.example;

/** Safe launch result returned to the JavaFX layer instead of throwing into UI code. */
public record LaunchResult(boolean success, String message, Throwable error) {
    public LaunchResult {
        message = message == null || message.isBlank()
                ? (success ? "Запуск выполнен" : "Не удалось выполнить запуск")
                : message.trim();
    }

    public static LaunchResult success(String message) {
        return new LaunchResult(true, message, null);
    }

    public static LaunchResult failure(String message) {
        return new LaunchResult(false, message, null);
    }

    public static LaunchResult failure(String message, Throwable error) {
        return new LaunchResult(false, message, error);
    }
}
