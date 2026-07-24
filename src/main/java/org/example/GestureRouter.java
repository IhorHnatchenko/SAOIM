package org.example;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Applies the global swipe rules to the current overlay state.
 *
 * MouseHookHandler only recognizes the physical left-button gesture. This
 * class decides whether it opens, closes, or returns the overlay to the
 * standard menu.
 */
public final class GestureRouter {

    private final Supplier<MenuState> stateSupplier;
    private final DesktopContextDetector desktopContextDetector;
    private final BiConsumer<Integer, Integer> showStandardMenuAction;
    private final Runnable hideStandardMenuAction;

    public GestureRouter(
            Supplier<MenuState> stateSupplier,
            DesktopContextDetector desktopContextDetector,
            BiConsumer<Integer, Integer> showStandardMenuAction,
            Runnable hideStandardMenuAction
    ) {
        this.stateSupplier = Objects.requireNonNull(stateSupplier, "stateSupplier");
        this.desktopContextDetector = Objects.requireNonNull(
                desktopContextDetector,
                "desktopContextDetector"
        );
        this.showStandardMenuAction = Objects.requireNonNull(
                showStandardMenuAction,
                "showStandardMenuAction"
        );
        this.hideStandardMenuAction = Objects.requireNonNull(
                hideStandardMenuAction,
                "hideStandardMenuAction"
        );
    }

    public void handleOpenGesture(int activationX, int activationY) {
        MenuState state = getCurrentState();

        switch (state) {
            case PROFILE -> showStandardMenuAction.accept(activationX, activationY);
            case STANDARD_MENU -> hideStandardMenuAction.run();
            case HIDDEN -> {
                if (desktopContextDetector.isDesktopActive()) {
                    showStandardMenuAction.accept(activationX, activationY);
                }
            }
        }
    }

    private MenuState getCurrentState() {
        MenuState state = stateSupplier.get();
        return state == null ? MenuState.HIDDEN : state;
    }
}
