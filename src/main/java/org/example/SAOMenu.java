package org.example;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.List;
import java.util.Stack;

/**
 * Single overlay window that switches between the standard menu and profile.
 * Step 10 adds coordinated state transitions and the final scene theme.
 */
public class SAOMenu {
    private static final double MENU_WIDTH = 300;
    private static final double MENU_HEIGHT = 520;

    private final Stack<List<MenuNode>> history = new Stack<>();
    private final MenuScreenContext screenContext = new MenuScreenContext();
    private final MenuStateController stateController =
            new MenuStateController(this::applyState);
    private final OverlayAnimationService animationService =
            new OverlayAnimationService();

    private Stage mainStage;
    private Stage dummyOwner;
    private Pane rootPane;
    private VBox menuContainer;
    private ProfileView profileView;
    private OrbitAnimationSupport orbitAnimationSupport;
    private List<MenuNode> rootMenu = List.of();
    private UserSession currentSession = UserSession.guest();
    private String currentActiveUser = "Guest";
    private MenuState renderedState = MenuState.HIDDEN;
    private PauseTransition hideFallback;
    private long hideRequestGeneration;

    public void init() {
        if (mainStage != null) {
            return;
        }

        dummyOwner = new Stage(StageStyle.UTILITY);
        dummyOwner.setOpacity(0);
        dummyOwner.setWidth(0);
        dummyOwner.setHeight(0);
        dummyOwner.setX(-100);
        dummyOwner.setY(-100);
        dummyOwner.show();

        menuContainer = createStandardMenuContainer();
        profileView = new ProfileView();
        profileView.setOnShortcutLaunched(this::hideMenu);
        profileView.setVisible(false);
        profileView.setManaged(false);

        rootPane = new Pane(menuContainer, profileView);
        rootPane.getStyleClass().add("sao-overlay-root");
        rootPane.setPickOnBounds(true);
        rootPane.setFocusTraversable(true);

        profileView.prefWidthProperty().bind(rootPane.widthProperty());
        profileView.prefHeightProperty().bind(rootPane.heightProperty());

        rootPane.setOnMouseReleased(event -> {
            boolean emptyOverlayArea = event.getTarget() == rootPane
                    || event.getTarget() == profileView;
            if (event.getButton() == MouseButton.SECONDARY && emptyOverlayArea) {
                event.consume();
                hideMenu();
            }
        });
        rootPane.setOnContextMenuRequested(event -> event.consume());

        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        loadTheme(scene);
        loadTheme(profileView);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSceneKeyPressed);

        mainStage = new Stage();
        mainStage.initOwner(dummyOwner);
        mainStage.initStyle(StageStyle.TRANSPARENT);
        mainStage.setAlwaysOnTop(true);
        mainStage.setScene(scene);
        mainStage.setTitle("SAO UI");
        mainStage.setOnHidden(event -> cancelHideFallback());

        orbitAnimationSupport = OrbitAnimationSupport.install(profileView.getOrbitPane());
        ModalAnimationSupport.install(profileView);
    }

    private VBox createStandardMenuContainer() {
        VBox container = new VBox(11);
        container.setPadding(new Insets(26, 18, 26, 18));
        container.setPrefSize(MENU_WIDTH, MENU_HEIGHT);
        container.setMinSize(MENU_WIDTH, MENU_HEIGHT);
        container.setMaxSize(MENU_WIDTH, MENU_HEIGHT);
        container.getStyleClass().add("standard-menu");

        container.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        container.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        return container;
    }

    private void buildInitialMenu() {
        MenuNode profileNode = new MenuNode(currentActiveUser, "◈", () -> {
            System.out.println("[Menu] Открываем профиль пользователя: " + currentActiveUser);
            showProfile();
        });

        MenuNode runNode = new MenuNode("Notepad", "✎", () -> {
            try {
                new ProcessBuilder("notepad.exe").start();
            } catch (Exception exception) {
                System.err.println(
                        "[Menu] Не удалось запустить Notepad: " + exception.getMessage()
                );
            }
        });

        MenuNode subMenu = new MenuNode("Files", "▱", null);
        subMenu.addChild(new MenuNode(
                "Documents", "▤", () -> System.out.println("Open Docs")
        ));
        subMenu.addChild(new MenuNode(
                "Pictures", "▧", () -> System.out.println("Open Pics")
        ));

        MenuNode settings = new MenuNode("Settings", "⚙", null);
        settings.addChild(new MenuNode("Log Out", "⎋", () -> {
            MainStarter.logoutUser();
            hideMenu();
        }));

        rootMenu = List.of(profileNode, runNode, subMenu, settings);
    }

    private void renderLevel(List<MenuNode> items) {
        menuContainer.getChildren().clear();

        if (!history.isEmpty()) {
            Button backButton = createMenuButton("Back", "⬅");
            backButton.getStyleClass().add("standard-menu-button--back");
            backButton.setOnAction(event -> {
                if (!history.isEmpty()) {
                    renderLevel(history.pop());
                }
            });
            menuContainer.getChildren().add(backButton);
        }

        for (MenuNode item : items) {
            Button button = createMenuButton(item.getTitle(), item.getIcon());
            boolean isRootProfile = history.isEmpty()
                    && !rootMenu.isEmpty()
                    && item == rootMenu.get(0);

            if (isRootProfile) {
                button.getStyleClass().add("standard-menu-button--profile");
            }

            button.setOnAction(event -> {
                if (item.hasChildren()) {
                    history.push(items);
                    renderLevel(item.getChildren());
                } else if (item.getAction() != null) {
                    item.getAction().run();
                    if (!isRootProfile) {
                        hideMenu();
                    }
                }
            });
            menuContainer.getChildren().add(button);
        }
    }

    private Button createMenuButton(String text, String icon) {
        String prefix = icon == null || icon.isBlank() ? "" : icon + "  ";
        Button button = new Button(prefix + text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("standard-menu-button");
        return button;
    }

    public void showMenu(UserSession session, double mouseX, double mouseY) {
        currentSession = session == null ? UserSession.guest() : session;
        currentActiveUser = normalizeUsername(currentSession.getUsername());

        if (mainStage == null) {
            init();
        }

        if (stateController.getState() == MenuState.HIDDEN) {
            screenContext.captureIfAbsent(mouseX, mouseY);
        }

        applyScreenBounds();
        history.clear();
        buildInitialMenu();
        renderLevel(rootMenu);
        profileView.setSession(currentSession);

        if (stateController.getState() == MenuState.PROFILE) {
            returnToStandardMenu();
        } else {
            stateController.showStandardMenu();
            requestOverlayFocus();
        }
    }

    /** Compatibility overload for older callers. */
    public void showMenu(String username, double mouseX, double mouseY) {
        showMenu(UserSession.guest(username), mouseX, mouseY);
    }

    public void showMenu(String username) {
        showMenu(username, Double.NaN, Double.NaN);
    }

    public void showProfile() {
        if (mainStage == null
                || stateController.getState() != MenuState.STANDARD_MENU) {
            return;
        }
        profileView.loadProfileAsync(currentSession);
        stateController.showProfile();
        requestOverlayFocus();
    }

    /**
     * Returns from PROFILE to the standard menu without rebuilding the window
     * or changing the monitor selected for the current overlay session.
     */
    public void returnToStandardMenu() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::returnToStandardMenu);
            return;
        }
        if (mainStage == null || stateController.getState() == MenuState.HIDDEN) {
            return;
        }

        history.clear();
        buildInitialMenu();
        renderLevel(rootMenu);

        if (stateController.getState() == MenuState.PROFILE) {
            stateController.showStandardMenu();
        } else {
            // Defensive repair for a previous interrupted transition.
            applyStateImmediately(MenuState.STANDARD_MENU);
        }
        requestOverlayFocus();
    }

    public void focusMenu() {
        if (mainStage == null || stateController.getState() == MenuState.HIDDEN) {
            return;
        }

        if (stateController.getState() == MenuState.STANDARD_MENU
                && (!menuContainer.isVisible() || profileView.isVisible())) {
            // A repeated global gesture also repairs a visually interrupted
            // PROFILE -> STANDARD_MENU transition.
            applyStateImmediately(MenuState.STANDARD_MENU);
        } else {
            prepareVisibleStage();
        }
        requestOverlayFocus();
    }

    public void hideMenu() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::hideMenu);
            return;
        }
        if (mainStage == null) {
            return;
        }

        long requestGeneration = ++hideRequestGeneration;
        cancelHideFallback();

        if (stateController.getState() == MenuState.HIDDEN) {
            // Defensive repair for a visible Stage whose logical state was
            // already switched to HIDDEN by an interrupted transition.
            applyStateImmediately(MenuState.HIDDEN);
            return;
        }

        stateController.hide();

        if (!mainStage.isShowing()) {
            return;
        }

        hideFallback = new PauseTransition(AnimationPreferences.duration(750.0));
        hideFallback.setOnFinished(event -> {
            hideFallback = null;
            if (requestGeneration != hideRequestGeneration) {
                return;
            }
            if (stateController.getState() == MenuState.HIDDEN
                    && mainStage != null
                    && mainStage.isShowing()) {
                System.err.println(
                        "[Navigation] Анимация закрытия не завершилась. "
                                + "Принудительно скрываем overlay."
                );
                applyStateImmediately(MenuState.HIDDEN);
            }
        });
        hideFallback.play();
    }

    public MenuState getCurrentState() {
        return stateController.getState();
    }

    public boolean isVisible() {
        return stateController.isVisible();
    }

    private void applyState(MenuState state) {
        if (mainStage == null) {
            return;
        }

        MenuState previous = renderedState;
        renderedState = state;

        try {
            switch (state) {
                case HIDDEN -> {
                    profileView.cancelProfileLoad();
                    profileView.resetOrbitNavigation();
                    screenContext.reset();
                    animationService.hide(
                            previous,
                            mainStage,
                            menuContainer,
                            profileView,
                            () -> System.out.println("[Navigation] HIDDEN")
                    );
                }
                case STANDARD_MENU -> {
                    cancelHideFallback();
                    prepareVisibleStage();
                    animationService.showStandardMenu(
                            previous,
                            mainStage,
                            menuContainer,
                            profileView
                    );
                    requestOverlayFocus();
                    System.out.println("[Navigation] STANDARD_MENU");
                }
                case PROFILE -> {
                    cancelHideFallback();
                    prepareVisibleStage();
                    animationService.showProfile(
                            previous,
                            mainStage,
                            menuContainer,
                            profileView
                    );
                    requestOverlayFocus();
                    System.out.println("[Navigation] PROFILE");
                }
            }
        } catch (RuntimeException exception) {
            System.err.println(
                    "[Navigation] Ошибка визуального перехода к " + state
                            + ". Применяем состояние без анимации: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
            applyStateImmediately(state);
        }
    }

    private void applyStateImmediately(MenuState state) {
        animationService.stopAndReset(menuContainer, profileView);
        renderedState = state;

        switch (state) {
            case HIDDEN -> {
                cancelHideFallback();
                animationService.forceHidden(
                        mainStage,
                        menuContainer,
                        profileView
                );
            }
            case STANDARD_MENU -> {
                prepareVisibleStage();
                animationService.forceStandardMenu(
                        mainStage,
                        menuContainer,
                        profileView
                );
                requestOverlayFocus();
            }
            case PROFILE -> {
                prepareVisibleStage();
                animationService.forceProfile(
                        mainStage,
                        menuContainer,
                        profileView
                );
                requestOverlayFocus();
            }
        }
    }

    private void handleSceneKeyPressed(KeyEvent event) {
        if (event == null) {
            return;
        }

        MenuState state = stateController.getState();
        if (state == MenuState.PROFILE && profileView.handleHistoryShortcut(event)) {
            event.consume();
            return;
        }

        if (event.getCode() != KeyCode.ESCAPE) {
            return;
        }

        event.consume();
        if (state == MenuState.PROFILE) {
            if (!profileView.handleEscape()) {
                returnToStandardMenu();
            }
        } else if (state == MenuState.STANDARD_MENU) {
            hideMenu();
        }
    }

    private void prepareVisibleStage() {
        applyScreenBounds();
        if (!mainStage.isShowing()) {
            mainStage.show();
        }
        mainStage.requestFocus();
    }

    private void applyScreenBounds() {
        Rectangle2D bounds = screenContext.getBoundsOrPrimary();
        mainStage.setX(bounds.getMinX());
        mainStage.setY(bounds.getMinY());
        mainStage.setWidth(bounds.getWidth());
        mainStage.setHeight(bounds.getHeight());

        menuContainer.setLayoutX(50);
        menuContainer.setLayoutY(
                Math.max(20, (bounds.getHeight() - MENU_HEIGHT) / 2.0)
        );
    }

    private void requestOverlayFocus() {
        if (mainStage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (mainStage != null && mainStage.isShowing()) {
                mainStage.toFront();
                mainStage.requestFocus();
                rootPane.requestFocus();
            }
        });
    }

    private void cancelHideFallback() {
        if (hideFallback != null) {
            hideFallback.stop();
            hideFallback = null;
        }
    }

    private void loadTheme(Scene scene) {
        URL theme = SAOMenu.class.getResource("/org/example/saoim-theme.css");
        if (theme != null) {
            scene.getStylesheets().add(theme.toExternalForm());
        } else {
            System.err.println("[Theme] Не найден /org/example/saoim-theme.css");
        }
    }

    private void loadTheme(Pane parent) {
        URL theme = SAOMenu.class.getResource("/org/example/saoim-theme.css");
        if (theme != null) {
            parent.getStylesheets().add(theme.toExternalForm());
        }
    }

    private String normalizeUsername(String username) {
        return username == null || username.isBlank() ? "Guest" : username;
    }
}
