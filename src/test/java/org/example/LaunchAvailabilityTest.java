package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchAvailabilityTest {
    @Test
    void factoryMethodsCreateMutuallyExclusiveStates() {
        LaunchAvailability available = LaunchAvailability.available("  готово  ");
        LaunchAvailability checking = LaunchAvailability.checking("проверка");
        LaunchAvailability unavailable = LaunchAvailability.unavailable("нет файла");

        assertTrue(available.available());
        assertFalse(available.checking());
        assertEquals("готово", available.message());

        assertFalse(checking.available());
        assertTrue(checking.checking());

        assertFalse(unavailable.available());
        assertFalse(unavailable.checking());
    }

    @Test
    void rejectsAvailableAndCheckingAtTheSameTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LaunchAvailability(true, true, "invalid")
        );
    }
}
