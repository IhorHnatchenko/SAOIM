package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Temporary profile screen used by step 1 to validate navigation.
 * The real profile card and circular menu will replace these placeholders later.
 */
public final class ProfileView extends Pane {
    private static final double CARD_WIDTH = 285;
    private static final double CARD_HEIGHT = 205;

    private final Label usernameLabel = new Label("Guest");
    private final VBox profileCard;
    private final StackPane circlePlaceholder;

    public ProfileView() {
        setPickOnBounds(false);

        Label sectionLabel = new Label("ПРОФИЛЬ");
        sectionLabel.setStyle("-fx-text-fill: #7fcfff; -fx-font-size: 13px;");

        usernameLabel.setStyle("-fx-text-fill: #55d7ff; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label titleLabel = new Label("Энтузиаст");
        titleLabel.setStyle("-fx-text-fill: #afc7e6; -fx-font-size: 14px;");

        Label levelLabel = new Label("Уровень 1");
        levelLabel.setStyle("-fx-text-fill: #8fe6ff; -fx-font-size: 14px;");

        Label xpLabel = new Label("0 / 1000 XP");
        xpLabel.setStyle("-fx-text-fill: #7fa6c8; -fx-font-size: 12px;");

        profileCard = new VBox(10, sectionLabel, usernameLabel, titleLabel, levelLabel, xpLabel);
        profileCard.setPadding(new Insets(20));
        profileCard.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        profileCard.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        profileCard.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        profileCard.setStyle(
                "-fx-background-color: rgba(3, 20, 39, 0.94);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(80, 205, 255, 0.85);" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 12;"
        );

        Label placeholderTitle = new Label("КОМАНДНЫЙ КРИСТАЛЛ");
        placeholderTitle.setStyle("-fx-text-fill: #62d8ff; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label placeholderSubtitle = new Label("Навигация подключена. Орбитальное меню будет добавлено на следующем этапе.");
        placeholderSubtitle.setWrapText(true);
        placeholderSubtitle.setMaxWidth(420);
        placeholderSubtitle.setAlignment(Pos.CENTER);
        placeholderSubtitle.setStyle("-fx-text-fill: #9bb8d3; -fx-font-size: 14px;");

        VBox centerText = new VBox(12, placeholderTitle, placeholderSubtitle);
        centerText.setAlignment(Pos.CENTER);

        circlePlaceholder = new StackPane(centerText);
        circlePlaceholder.setStyle(
                "-fx-background-color: rgba(2, 17, 35, 0.83);" +
                "-fx-background-radius: 1000;" +
                "-fx-border-color: rgba(44, 173, 255, 0.82);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 1000;"
        );

        consumeSecondaryClicks(profileCard);
        consumeSecondaryClicks(circlePlaceholder);

        getChildren().addAll(profileCard, circlePlaceholder);
    }

    public void setUsername(String username) {
        usernameLabel.setText(username == null || username.isBlank() ? "Guest" : username);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();

        profileCard.resizeRelocate(30, 30, CARD_WIDTH, CARD_HEIGHT);

        double diameter = Math.max(360, Math.min(width * 0.58, height * 0.72));
        double circleX = Math.max(350, (width - diameter) / 2.0);
        double circleY = (height - diameter) / 2.0;
        circlePlaceholder.resizeRelocate(circleX, circleY, diameter, diameter);
    }

    private void consumeSecondaryClicks(javafx.scene.Node node) {
        node.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        node.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
    }
}
