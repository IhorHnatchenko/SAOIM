package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.List;
import java.util.Stack;

public class SAOMenu {

    private Stage mainStage;
    private Stage dummyOwner;
    private VBox menuContainer;
    private Pane rootPane;
    private Stack<List<MenuNode>> history = new Stack<>();
    private List<MenuNode> rootMenu;
    private String currentActiveUser = "Guest";

    private final double MENU_WIDTH = 280;
    private final double MENU_HEIGHT = 500;

    private final String BASE_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-alignment: center-left; -fx-padding: 10 20 10 20;";
    private final String HOVER_STYLE = "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: orange; -fx-font-size: 16px; -fx-alignment: center-left; -fx-padding: 10 20 10 20;";
    private final String PROFILE_STYLE = "-fx-background-color: rgba(255,153,0,0.1); -fx-text-fill: #ff9900; -fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-padding: 12 20 12 20; -fx-border-color: rgba(255,153,0,0.3); -fx-border-width: 0 0 1 0;";

    public void init() {
        if (mainStage != null) return;

        dummyOwner = new Stage(StageStyle.UTILITY);
        dummyOwner.setOpacity(0);
        dummyOwner.setWidth(0);
        dummyOwner.setHeight(0);
        dummyOwner.setX(-100);
        dummyOwner.show();

        menuContainer = new VBox(12);
        menuContainer.setPadding(new Insets(25, 15, 25, 15));
        menuContainer.setPrefSize(MENU_WIDTH, MENU_HEIGHT);
        menuContainer.setStyle("-fx-background-color: rgba(30, 30, 30, 0.95); -fx-background-radius: 15; -fx-border-color: white; -fx-border-width: 1;");

        menuContainer.setOnMousePressed(e -> { if (e.getButton() == MouseButton.SECONDARY) e.consume(); });
        menuContainer.setOnMouseReleased(e -> { if (e.getButton() == MouseButton.SECONDARY) e.consume(); });

        rootPane = new Pane(menuContainer);
        rootPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.01);");
        rootPane.setPickOnBounds(true);

        rootPane.setOnMousePressed(e -> { if (e.getButton() == MouseButton.SECONDARY) e.consume(); });
        rootPane.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
                hideMenu();
            }
        });

        rootPane.setOnContextMenuRequested(e -> e.consume());

        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);

        mainStage = new Stage();
        mainStage.initOwner(dummyOwner);
        mainStage.initStyle(StageStyle.TRANSPARENT);
        mainStage.setAlwaysOnTop(true);
        mainStage.setScene(scene);
        mainStage.setTitle("SAO UI");
    }

    private void buildInitialMenu(){
        MenuNode profileNode = new MenuNode(currentActiveUser, "👤", () -> {
            System.out.println("[Menu] Клик по профилю пользователя: " + currentActiveUser);
        });

        MenuNode runNode = new MenuNode("Notepad", "✎", () -> {
            try { new ProcessBuilder("notepad.exe").start(); } catch (Exception exception) {}
        });

        MenuNode subMenu = new MenuNode("Files", "\uD83D\uDCC2", null);
        subMenu.addChild(new MenuNode("Documents", "\uD83D\uDCC4", () -> System.out.println("Open Docs")));
        subMenu.addChild(new MenuNode("Pictures", "\uD83D\uDDBC", () -> System.out.println("Open Pics")));

        MenuNode settings = new MenuNode("Settings", "⚙", null);
        settings.addChild(new MenuNode("Log Out", "⎋", () -> {
            MainStarter.logoutUser();
            hideMenu();
        }));

        rootMenu = List.of(profileNode, runNode, subMenu, settings);
        renderLevel(rootMenu);
    }

    private void renderLevel(List<MenuNode> items) {
        menuContainer.getChildren().clear();

        if (!history.isEmpty()) {
            Button backButton = createMenuButton("Back", "⬅");
            backButton.setOnAction(e -> {
                if (!history.isEmpty()){
                    renderLevel(history.pop());
                }
            });
            menuContainer.getChildren().add(backButton);
        }

        for (MenuNode item : items) {
            Button button = createMenuButton(item.getTitle(), item.getIcon());

            if (history.isEmpty() && item == rootMenu.get(0)) {
                button.setStyle(PROFILE_STYLE);
                button.setOnMouseExited(e -> button.setStyle(PROFILE_STYLE));
            }

            button.setOnAction(e -> {
                if (item.hasChildren()) {
                    history.push(items);
                    renderLevel(item.getChildren());
                } else if (item.getAction() != null) {
                    item.getAction().run();
                    if (item != rootMenu.get(0)) hideMenu();
                }
            });
            menuContainer.getChildren().add(button);
        }
    }

    private Button createMenuButton (String text, String icon) {
        Button button = new Button(icon + " " + text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(BASE_STYLE);
        button.setOnMouseEntered(e -> button.setStyle(HOVER_STYLE));
        button.setOnMouseExited(e -> button.setStyle(BASE_STYLE));
        return button;
    }

    public void showMenu(String username){
        this.currentActiveUser = username;
        if (mainStage == null) init();

        buildInitialMenu();
        history.clear();
        renderLevel(rootMenu);

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        mainStage.setX(bounds.getMinX());
        mainStage.setY(bounds.getMinY());
        mainStage.setWidth(bounds.getWidth());
        mainStage.setHeight(bounds.getHeight());

        menuContainer.setLayoutX(50);
        menuContainer.setLayoutY((bounds.getHeight() - MENU_HEIGHT) / 2);

        mainStage.show();
        mainStage.requestFocus();
    }

    public void hideMenu(){
        if (mainStage != null) mainStage.hide();
    }
}