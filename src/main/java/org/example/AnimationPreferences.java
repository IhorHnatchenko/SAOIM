package org.example;

import javafx.util.Duration;

/**
 * Central animation settings for the overlay.
 *
 * <p>Animations are enabled by default. They can be disabled with
 * {@code -Dsaoim.animations=false}. Their speed can be adjusted with
 * {@code -Dsaoim.animationScale=0.75}.</p>
 */
public final class AnimationPreferences {
    private static final double MIN_SCALE = 0.25;
    private static final double MAX_SCALE = 3.0;

    private AnimationPreferences() {
    }

    public static boolean isEnabled() {
        String property = System.getProperty("saoim.animations", "true");
        return !"false".equalsIgnoreCase(property)
                && !"0".equals(property)
                && !"off".equalsIgnoreCase(property);
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
}
