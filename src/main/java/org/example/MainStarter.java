package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainStarter extends Application {

    private static boolean isUserAuthorized = false;
    private static String currentUsername = "Guest";
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
            System.out.println("[System] Авторизация успешна. Доступ к SAOMenu открыт.");
            saoMenu.showMenu(currentUsername, pendingTriggerX, pendingTriggerY);
        });

        // Сквозная проверка автологина при холодном старте приложения.
        if (authWindow.hasSavedSession()) {
            isUserAuthorized = true;
            String[] saved = new File("sao_config.properties").exists()
                    ? loadUserFromConfig()
                    : null;

            if (saved != null && saved.length > 0 && saved[0] != null) {
                currentUsername = saved[0];
            }

            System.out.println(
                    "[System] Обнаружена сохраненная сессия пользователя: " + currentUsername
            );
        } else {
            isUserAuthorized = false;
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
                    MainStarter::focusOverlay
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
                    "[System] КРИТИЧЕСКАЯ ОШИБКА: Не удалось запустить хук мыши: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Requests the standard menu after GestureRouter has approved the gesture.
     * The activation point is stored only for a new HIDDEN menu session; a
     * PROFILE -> STANDARD_MENU transition keeps the already selected monitor.
     */
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
                    focusOverlayNow();
                    return;
                }

                if (!isUserAuthorized) {
                    // Authorization is relevant only to opening a hidden menu.
                    // A PROFILE state can exist only for an authorized user.
                    System.out.println("[System] Жест принят. Требуется авторизация...");
                    authWindow.show();
                    return;
                }

                if (currentState == MenuState.PROFILE) {
                    System.out.println("[System] Жест возвращает профиль в стандартное меню.");
                    saoMenu.showMenu(currentUsername, Double.NaN, Double.NaN);
                } else {
                    System.out.println(
                            "[System] Жест принят. Открываем стандартное меню для "
                                    + currentUsername
                    );
                    saoMenu.showMenu(currentUsername, pendingTriggerX, pendingTriggerY);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public static void focusOverlay() {
        Platform.runLater(MainStarter::focusOverlayNow);
    }

    private static void focusOverlayNow() {
        if (saoMenu != null) {
            saoMenu.focusMenu();
        }
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username == null || username.isBlank() ? "Guest" : username;
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
        pendingTriggerX = Double.NaN;
        pendingTriggerY = Double.NaN;

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

    private String[] loadUserFromConfig() {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream("sao_config.properties")) {
            properties.load(inputStream);
            return new String[]{properties.getProperty("username")};
        } catch (Exception exception) {
            return null;
        }
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
