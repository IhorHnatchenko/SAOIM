package org.example;

/** Result of checking whether a shortcut can be launched on this computer. */
public record LaunchAvailability(boolean available, boolean checking, String message) {
    public LaunchAvailability {
        message = message == null ? "" : message.trim();
        if (available && checking) {
            throw new IllegalArgumentException("Availability cannot be available and checking at once.");
        }
    }

    public static LaunchAvailability available(String message) {
        return new LaunchAvailability(true, false, message);
    }

    public static LaunchAvailability unavailable(String message) {
        return new LaunchAvailability(false, false, message);
    }

    public static LaunchAvailability checking(String message) {
        return new LaunchAvailability(false, true, message);
    }
}
