package org.example;

import javafx.util.Duration;

/** Central animation and performance settings for the overlay. */
public final class AnimationPreferences {
    private static final double MIN_SCALE = 0.25;
    private static final double MAX_SCALE = 3.0;

    private AnimationPreferences() {
    }

    /** Disable every transition with -Dsaoim.animations=false. */
    public static boolean isEnabled() {
        return readBoolean("saoim.animations", true);
    }

    /**
     * Continuous ring/glow animations are disabled by default because they can
     * keep the JavaFX pulse and GPU busy even while the user is only editing
     * data. They may be restored with -Dsaoim.ambientAnimations=true.
     */
    public static boolean isAmbientEnabled() {
        return isEnabled() && readBoolean("saoim.ambientAnimations", false);
    }

    public static double getDurationScale() {
        String raw = System.getProperty("saoim.animationScale", "1.0");
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) {
                return 1.0;
            }
            return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    public static Duration duration(double milliseconds) {
        return Duration.millis(Math.max(1.0, milliseconds * getDurationScale()));
    }

    private static boolean readBoolean(String key, boolean defaultValue) {
        String fallback = defaultValue ? "true" : "false";
        String value = System.getProperty(key, fallback).trim();
        return switch (value.toLowerCase()) {
            case "true", "1", "on", "yes" -> true;
            case "false", "0", "off", "no" -> false;
            default -> defaultValue;
        };
    }
}
