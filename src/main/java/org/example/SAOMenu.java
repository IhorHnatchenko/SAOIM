package org.example;

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

import java.util.List;
import java.util.Stack;

/** Single overlay window that switches between the standard menu and profile. */
public class SAOMenu {
    private static final double MENU_WIDTH = 280;
    private static final double MENU_HEIGHT = 500;

    private static final String BASE_STYLE =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 16px;" +
                    "-fx-alignment: center-left;" +
                    "-fx-padding: 10 20 10 20;";

    private static final String HOVER_STYLE =
            "-fx-background-color: rgba(255,255,255,0.2);" +
                    "-fx-text-fill: orange;" +
                    "-fx-font-size: 16px;" +
                    "-fx-alignment: center-left;" +
                    "-fx-padding: 10 20 10 20;";

    private static final String PROFILE_STYLE =
            "-fx-background-color: rgba(255,153,0,0.1);" +
                    "-fx-text-fill: #ff9900;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-alignment: center-left;" +
                    "-fx-padding: 12 20 12 20;" +
                    "-fx-border-color: rgba(255,153,0,0.3);" +
                    "-fx-border-width: 0 0 1 0;";

    private final Stack<List<MenuNode>> history = new Stack<>();
    private final MenuScreenContext screenContext = new MenuScreenContext();
    private final MenuStateController stateController =
            new MenuStateController(this::applyState);

    private Stage mainStage;
    private Stage dummyOwner;
    private Pane rootPane;
    private VBox menuContainer;
    private ProfileView profileView;
    private List<MenuNode> rootMenu;
    private UserSession currentSession = UserSession.guest();
    private String currentActiveUser = "Guest";

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
        profileView.setVisible(false);
        profileView.setManaged(false);

        rootPane = new Pane(menuContainer, profileView);
        rootPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.01);");
        rootPane.setPickOnBounds(true);

        profileView.prefWidthProperty().bind(rootPane.widthProperty());
        profileView.prefHeightProperty().bind(rootPane.heightProperty());

        rootPane.setOnMouseReleased(event -> {
            boolean emptyOverlayArea =
                    event.getTarget() == rootPane || event.getTarget() == profileView;
            if (event.getButton() == MouseButton.SECONDARY && emptyOverlayArea) {
                event.consume();
                hideMenu();
            }
        });
        rootPane.setOnContextMenuRequested(event -> event.consume());

        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ESCAPE) {
                return;
            }
            event.consume();
            boolean handledByOrbit =
                    stateController.getState() == MenuState.PROFILE &&
                            profileView.handleEscape();
            if (!handledByOrbit) {
                stateController.handleEscape();
            }
        });

        mainStage = new Stage();
        mainStage.initOwner(dummyOwner);
        mainStage.initStyle(StageStyle.TRANSPARENT);
        mainStage.setAlwaysOnTop(true);
        mainStage.setScene(scene);
        mainStage.setTitle("SAO UI");
    }

    private VBox createStandardMenuContainer() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(25, 15, 25, 15));
        container.setPrefSize(MENU_WIDTH, MENU_HEIGHT);
        container.setMinSize(MENU_WIDTH, MENU_HEIGHT);
        container.setMaxSize(MENU_WIDTH, MENU_HEIGHT);
        container.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.95);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: white;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 15;"
        );
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
        MenuNode profileNode = new MenuNode(currentActiveUser, "", () -> {
            System.out.println(
                    "[Menu] Открываем профиль пользователя: " + currentActiveUser
            );
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

        MenuNode subMenu = new MenuNode("Files", "", null);
        subMenu.addChild(new MenuNode(
                "Documents",
                "",
                () -> System.out.println("Open Docs")
        ));
        subMenu.addChild(new MenuNode(
                "Pictures",
                "",
                () -> System.out.println("Open Pics")
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
            backButton.setOnAction(event -> {
                if (!history.isEmpty()) {
                    renderLevel(history.pop());
                }
            });
            menuContainer.getChildren().add(backButton);
        }

        for (MenuNode item : items) {
            Button button = createMenuButton(item.getTitle(), item.getIcon());
            boolean isRootProfile = history.isEmpty() && item == rootMenu.get(0);

            if (isRootProfile) {
                button.setStyle(PROFILE_STYLE);
                button.setOnMouseExited(event -> button.setStyle(PROFILE_STYLE));
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
        String prefix = icon == null || icon.isBlank() ? "" : icon + " ";
        Button button = new Button(prefix + text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(BASE_STYLE);
        button.setOnMouseEntered(event -> button.setStyle(HOVER_STYLE));
        button.setOnMouseExited(event -> button.setStyle(BASE_STYLE));
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
        stateController.showStandardMenu();
        focusMenu();
    }

    /** Compatibility overload for older callers. */
    public void showMenu(String username, double mouseX, double mouseY) {
        showMenu(UserSession.guest(username), mouseX, mouseY);
    }

    public void showMenu(String username) {
        showMenu(username, Double.NaN, Double.NaN);
    }

    public void showProfile() {
        if (mainStage == null || stateController.getState() != MenuState.STANDARD_MENU) {
            return;
        }
        profileView.loadProfileAsync(currentSession);
        stateController.showProfile();
        focusMenu();
    }

    public void focusMenu() {
        if (mainStage == null || stateController.getState() == MenuState.HIDDEN) {
            return;
        }
        ensureStageVisible();
        mainStage.toFront();
        mainStage.requestFocus();
    }

    public void hideMenu() {
        stateController.hide();
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

        switch (state) {
            case HIDDEN -> {
                menuContainer.setVisible(false);
                menuContainer.setManaged(false);
                profileView.setVisible(false);
                profileView.setManaged(false);
                profileView.cancelProfileLoad();
                profileView.resetOrbitNavigation();
                mainStage.hide();
                screenContext.reset();
                System.out.println("[Navigation] HIDDEN");
            }
            case STANDARD_MENU -> {
                profileView.setVisible(false);
                profileView.setManaged(false);
                menuContainer.setVisible(true);
                menuContainer.setManaged(true);
                ensureStageVisible();
                System.out.println("[Navigation] STANDARD_MENU");
            }
            case PROFILE -> {
                menuContainer.setVisible(false);
                menuContainer.setManaged(false);
                profileView.setVisible(true);
                profileView.setManaged(true);
                ensureStageVisible();
                System.out.println("[Navigation] PROFILE");
            }
        }
    }

    private void ensureStageVisible() {
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

    private String normalizeUsername(String username) {
        return username == null || username.isBlank() ? "Guest" : username;
    }
}
