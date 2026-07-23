package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainStarter extends Application {
    private static boolean isUserAuthorized;
    private static String currentUsername = "Guest";
    private static UserSession currentSession = UserSession.guest();
    private static double pendingTriggerX = Double.NaN;
    private static double pendingTriggerY = Double.NaN;

    private static AuthWindow authWindow;
    private static SAOMenu saoMenu;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        saoMenu = new SAOMenu();
        authWindow = new AuthWindow(() -> {
            isUserAuthorized = true;
            synchronizeAuthenticatedSession();
            System.out.println(
                    "[System] Авторизация успешна для accountId=" +
                            currentSession.getAccountId() + ". Доступ к SAOMenu открыт."
            );
            saoMenu.showMenu(currentSession, pendingTriggerX, pendingTriggerY);
        });

        // hasSavedSession() uses AuthService.loginUser(), which also prepares
        // the account-aware UserSession used below.
        if (authWindow.hasSavedSession()) {
            isUserAuthorized = true;
            synchronizeAuthenticatedSession();
            System.out.println(
                    "[System] Обнаружена сохраненная сессия пользователя: " +
                            currentSession.getUsername()
            );
        } else {
            isUserAuthorized = false;
            currentSession = UserSession.guest();
            currentUsername = "Guest";
            System.out.println("[System] Сохраненной сессии нет. Требуется ручной ввод.");
        }

        registerGlobalMouseHook();
    }

    private void registerGlobalMouseHook() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            GestureRouter gestureRouter = new GestureRouter(
                    MainStarter::getMenuState,
                    new DesktopContextDetector(),
                    MainStarter::triggerMenu,
                    MainStarter::hideOverlay
            );
            MouseHookHandler mouseHook = new MouseHookHandler(
                    gestureRouter,
                    new ScreenService()
            );
            GlobalScreen.addNativeMouseListener(mouseHook);
            GlobalScreen.addNativeMouseMotionListener(mouseHook);
            System.out.println("[System] Глобальный хук мыши успешно запущен!");
        } catch (NativeHookException exception) {
            System.err.println(
                    "[System] КРИТИЧЕСКАЯ ОШИБКА: Не удалось запустить хук мыши: " +
                            exception.getMessage()
            );
        }
    }

    public static void triggerMenu(int mouseX, int mouseY) {
        MenuState stateAtRequest = getMenuState();
        if (stateAtRequest == MenuState.HIDDEN) {
            pendingTriggerX = mouseX;
            pendingTriggerY = mouseY;
        }

        Platform.runLater(() -> {
            try {
                MenuState currentState = getMenuState();
                if (currentState == MenuState.STANDARD_MENU) {
                    hideOverlayNow();
                    return;
                }

                if (!isUserAuthorized) {
                    System.out.println("[System] Жест принят. Требуется авторизация...");
                    authWindow.show();
                    return;
                }

                if (currentState == MenuState.PROFILE) {
                    System.out.println(
                            "[System] Жест возвращает профиль в стандартное меню."
                    );
                    saoMenu.showMenu(currentSession, Double.NaN, Double.NaN);
                } else {
                    System.out.println(
                            "[System] Жест принят. Открываем стандартное меню для " +
                                    currentSession.getUsername()
                    );
                    saoMenu.showMenu(
                            currentSession,
                            pendingTriggerX,
                            pendingTriggerY
                    );
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * Closes the currently visible standard menu. The global mouse hook calls
     * this method from its own thread, therefore the actual UI operation is
     * always transferred to the JavaFX Application Thread.
     */
    public static void hideOverlay() {
        Platform.runLater(MainStarter::hideOverlayNow);
    }

    private static void hideOverlayNow() {
        if (saoMenu != null) {
            saoMenu.hideMenu();
        }
    }

    public static void focusOverlay() {
        Platform.runLater(MainStarter::focusOverlayNow);
    }

    private static void focusOverlayNow() {
        if (saoMenu != null) {
            saoMenu.focusMenu();
        }
    }

    /** Called by the current AuthWindow immediately before its success callback. */
    public static void setCurrentUsername(String username) {
        currentUsername = normalizeUsername(username);
        AuthService.getLastAuthenticatedSession()
                .filter(session -> session.getUsername().equalsIgnoreCase(currentUsername))
                .ifPresent(session -> currentSession = session);
    }

    public static UserSession getCurrentSession() {
        return currentSession;
    }

    public static MenuState getMenuState() {
        return saoMenu == null ? MenuState.HIDDEN : saoMenu.getCurrentState();
    }

    public static boolean isOverlayVisible() {
        return saoMenu != null && saoMenu.isVisible();
    }

    public static void logoutUser() {
        isUserAuthorized = false;
        currentUsername = "Guest";
        currentSession = UserSession.guest();
        pendingTriggerX = Double.NaN;
        pendingTriggerY = Double.NaN;
        AuthService.clearAuthenticatedSession();

        if (saoMenu != null) {
            saoMenu.hideMenu();
        }
        if (authWindow != null) {
            authWindow.clearSavedCredentials();
        }

        System.out.println(
                "[System] Пользователь вышел. Сессия и конфигурация автологина аннулированы."
        );
    }

    private static void synchronizeAuthenticatedSession() {
        currentSession = AuthService.getLastAuthenticatedSession()
                .orElseGet(() -> UserSession.guest(currentUsername));
        currentUsername = currentSession.getUsername();
    }

    private static String normalizeUsername(String username) {
        return username == null || username.isBlank() ? "Guest" : username.trim();
    }

    @Override
    public void stop() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (Exception ignored) {
            // The hook may already be unregistered during shutdown.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
