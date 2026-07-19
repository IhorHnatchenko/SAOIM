package org.example;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Owns the navigation rules between the main overlay states.
 * UI rendering is delegated through the stateChanged callback.
 */
public final class MenuStateController {

    private final Consumer<MenuState> stateChanged;

    /**
     * Read by the JNativeHook thread through GestureRouter and changed on the
     * JavaFX thread. volatile guarantees visibility of the latest enum value.
     */
    private volatile MenuState state = MenuState.HIDDEN;

    public MenuStateController(Consumer<MenuState> stateChanged) {
        this.stateChanged = Objects.requireNonNull(stateChanged, "stateChanged");
    }

    public MenuState getState() {
        return state;
    }

    public boolean isVisible() {
        return state != MenuState.HIDDEN;
    }

    public void showStandardMenu() {
        transitionTo(MenuState.STANDARD_MENU);
    }

    public void showProfile() {
        if (state == MenuState.STANDARD_MENU) {
            transitionTo(MenuState.PROFILE);
        }
    }

    public void hide() {
        transitionTo(MenuState.HIDDEN);
    }

    /**
     * PROFILE -> STANDARD_MENU -> HIDDEN.
     */
    public void handleEscape() {
        switch (state) {
            case PROFILE -> showStandardMenu();
            case STANDARD_MENU -> hide();
            case HIDDEN -> {
                // Nothing to close.
            }
        }
    }

    private void transitionTo(MenuState nextState) {
        Objects.requireNonNull(nextState, "nextState");
        if (state == nextState) {
            return;
        }

        state = nextState;
        stateChanged.accept(state);
    }
}
