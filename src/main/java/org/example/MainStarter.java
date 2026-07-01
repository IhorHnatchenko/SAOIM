package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainStarter extends Application {

    private static boolean isUserAuthorized = false;
    private static String currentUsername = "Guest";
    private static AuthWindow authWindow;
    private static SAOMenu saoMenu;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        saoMenu = new SAOMenu();
        authWindow = new AuthWindow(() -> {
            isUserAuthorized = true;
            System.out.println("[System] Авторизация успешна. Доступ к SAOMenu открыт.");
            saoMenu.showMenu(currentUsername);
        });

        // Сквозная проверка автологина при холодном старте приложения
        if (authWindow.hasSavedSession()) {
            isUserAuthorized = true;
            String[] saved = new java.io.File("sao_config.properties").exists() ? loadUserFromConfig() : null;
            if (saved != null) currentUsername = saved[0];
            System.out.println("[System] Обнаружена сохраненная сессия пользователя: " + currentUsername);
        } else {
            isUserAuthorized = false;
            System.out.println("[System] Сохраненной сессии нет. Требуется ручной ввод.");
        }

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            MouseHookHandler mouseHook = new MouseHookHandler();
            GlobalScreen.addNativeMouseListener(mouseHook);
            GlobalScreen.addNativeMouseMotionListener(mouseHook);
            System.out.println("[System] Глобальный хук мыши успешно запущен!");
        } catch (NativeHookException ex) {
            System.err.println("[System] КРИТИЧЕСКАЯ ОШИБКА: Не удалось запустить хук мыши: " + ex.getMessage());
        }
    }

    public static void triggerMenu(int mouseX, int mouseY) {
        Platform.runLater(() -> {
            try {
                if (!isUserAuthorized) {
                    System.out.println("[System] Жест принят. Требуется авторизация...");
                    authWindow.show();
                } else {
                    System.out.println("[System] Жест принят. Открываем SAOMenu для " + currentUsername);
                    saoMenu.showMenu(currentUsername);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    public static void logoutUser() {
        isUserAuthorized = false;
        currentUsername = "Guest";
        if (authWindow != null) {
            authWindow.clearSavedCredentials();
        }
        System.out.println("[System] Пользователь вышел. Сессия и конфигурация автологина аннулированы.");
    }

    private String[] loadUserFromConfig() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream in = new java.io.FileInputStream("sao_config.properties")) {
            props.load(in);
            return new String[]{props.getProperty("username")};
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void stop() {
        try { GlobalScreen.unregisterNativeHook(); } catch (Exception e) {}
    }

    public static void main(String[] args) { launch(args); }
}