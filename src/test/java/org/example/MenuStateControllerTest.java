package org.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuStateControllerTest {
    @Test
    void followsExpectedStateSequence() {
        List<MenuState> transitions = new ArrayList<>();
        MenuStateController controller = new MenuStateController(transitions::add);

        assertEquals(MenuState.HIDDEN, controller.getState());
        assertFalse(controller.isVisible());

        controller.showStandardMenu();
        controller.showProfile();
        controller.handleEscape();
        controller.handleEscape();

        assertEquals(
                List.of(
                        MenuState.STANDARD_MENU,
                        MenuState.PROFILE,
                        MenuState.STANDARD_MENU,
                        MenuState.HIDDEN
                ),
                transitions
        );
        assertFalse(controller.isVisible());
    }

    @Test
    void profileCanOnlyOpenFromStandardMenu() {
        List<MenuState> transitions = new ArrayList<>();
        MenuStateController controller = new MenuStateController(transitions::add);

        controller.showProfile();

        assertEquals(MenuState.HIDDEN, controller.getState());
        assertTrue(transitions.isEmpty());
    }

    @Test
    void duplicateTransitionDoesNotNotifyViewAgain() {
        List<MenuState> transitions = new ArrayList<>();
        MenuStateController controller = new MenuStateController(transitions::add);

        controller.showStandardMenu();
        controller.showStandardMenu();

        assertEquals(List.of(MenuState.STANDARD_MENU), transitions);
        assertTrue(controller.isVisible());
    }
}
