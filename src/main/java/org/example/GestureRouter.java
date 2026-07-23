package org.example;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Applies the global gesture rules to the current overlay state.
 *
 * MouseHookHandler only recognizes the physical gesture. This class decides
 * whether the gesture opens the hidden menu, returns PROFILE to the standard
 * menu, or closes an already visible standard menu.
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
            case PROFILE -> {
                System.out.println("[Gesture] PROFILE -> STANDARD_MENU");
                showStandardMenuAction.accept(activationX, activationY);
            }
            case STANDARD_MENU -> {
                System.out.println("[Gesture] STANDARD_MENU -> HIDDEN");
                hideStandardMenuAction.run();
            }
            case HIDDEN -> {
                if (!desktopContextDetector.isDesktopActive()) {
                    System.out.println(
                            "[Gesture] Жест проигнорирован: активным является не рабочий стол."
                    );
                    return;
                }

                System.out.println("[Gesture] Рабочий стол активен. Открываем стандартное меню.");
                showStandardMenuAction.accept(activationX, activationY);
            }
        }
    }


    /**
     * A native secondary-click fallback for the standard menu.
     *
     * Transparent JavaFX pixels are not guaranteed to receive mouse events on
     * every Windows/DPI configuration. JNativeHook sees the click regardless,
     * so a right click dismisses the visible standard menu reliably.
     */
    public void handleSecondaryClick(int screenX, int screenY) {
        if (getCurrentState() != MenuState.STANDARD_MENU) {
            return;
        }
        System.out.println(
                "[Gesture] Правая кнопка мыши: STANDARD_MENU -> HIDDEN "
                        + "(" + screenX + ", " + screenY + ")"
        );
        hideStandardMenuAction.run();
    }

    private MenuState getCurrentState() {
        MenuState state = stateSupplier.get();
        return state == null ? MenuState.HIDDEN : state;
    }
}
