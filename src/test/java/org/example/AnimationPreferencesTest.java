package org.example;

import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationPreferencesTest {
    @AfterEach
    void clearProperties() {
        System.clearProperty("saoim.animations");
        System.clearProperty("saoim.ambientAnimations");
        System.clearProperty("saoim.animationScale");
    }

    @Test
    void readsSupportedBooleanForms() {
        System.setProperty("saoim.animations", "yes");
        assertTrue(AnimationPreferences.isEnabled());

        System.setProperty("saoim.animations", "off");
        assertFalse(AnimationPreferences.isEnabled());
    }

    @Test
    void ambientAnimationRequiresMainAnimations() {
        System.setProperty("saoim.animations", "false");
        System.setProperty("saoim.ambientAnimations", "true");

        assertFalse(AnimationPreferences.isAmbientEnabled());
    }

    @Test
    void clampsAndAppliesDurationScale() {
        System.setProperty("saoim.animationScale", "0.1");
        assertEquals(0.25, AnimationPreferences.getDurationScale());
        assertEquals(Duration.millis(25), AnimationPreferences.duration(100));

        System.setProperty("saoim.animationScale", "10");
        assertEquals(3.0, AnimationPreferences.getDurationScale());

        System.setProperty("saoim.animationScale", "not-a-number");
        assertEquals(1.0, AnimationPreferences.getDurationScale());
    }
}
