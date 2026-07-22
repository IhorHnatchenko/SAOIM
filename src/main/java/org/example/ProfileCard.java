package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/** Profile card shown in the upper-left corner of ProfileView. */
public final class ProfileCard extends StackPane {
    private static final double DESIGN_WIDTH = 300;
    private static final double DESIGN_HEIGHT = 220;

    private final Label nicknameLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label levelLabel = new Label();
    private final Label xpLabel = new Label();
    private final ProgressBar xpBar = new ProgressBar();
    private final Label footerStatus = new Label("SAO USER PROFILE");

    public ProfileCard() {
        getStyleClass().add("profile-card");
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPadding(new Insets(16, 18, 16, 18));

        Label header = new Label("ПРОФИЛЬ");
        header.getStyleClass().add("profile-card__header");

        StackPane avatar = createAvatar();
        avatar.getStyleClass().add("profile-avatar");

        nicknameLabel.getStyleClass().add("profile-card__nickname");
        titleLabel.getStyleClass().add("profile-card__title");
        levelLabel.getStyleClass().add("profile-card__level");
        xpLabel.getStyleClass().add("profile-card__xp-text");

        xpBar.setMinWidth(138);
        xpBar.setPrefWidth(138);
        xpBar.setMaxWidth(138);
        xpBar.getStyleClass().add("profile-card__xp-bar");

        VBox data = new VBox(5, nicknameLabel, titleLabel, levelLabel, xpBar, xpLabel);
        data.setAlignment(Pos.CENTER_LEFT);
        data.setFillWidth(true);

        HBox content = new HBox(16, avatar, data);
        content.setAlignment(Pos.CENTER_LEFT);

        footerStatus.getStyleClass().add("profile-card__footer");

        VBox body = new VBox(10, header, content, footerStatus);
        body.setFillWidth(true);
        getChildren().add(body);

        consumeSecondaryClicks();
        setProfile(UserProfile.starter("Guest"));
    }

    public void setProfile(UserProfile profile) {
        UserProfile safeProfile = profile == null
                ? UserProfile.starter("Guest")
                : profile;

        nicknameLabel.setText(safeProfile.getNickname());
        titleLabel.setText(safeProfile.getTitle());
        levelLabel.setText("Уровень " + safeProfile.getLevel());
        xpLabel.setText(
                safeProfile.getCurrentXp() + " / " + safeProfile.getRequiredXp() + " XP"
        );
        xpBar.setProgress(safeProfile.getXpProgress());
        footerStatus.setText("SAO USER PROFILE");
        setOpacity(1.0);
    }

    public void showLoading(String username) {
        setProfile(UserProfile.starter(username));
        footerStatus.setText("ЗАГРУЗКА ПРОФИЛЯ...");
        setOpacity(0.82);
    }

    public void showLoadError(String username) {
        setProfile(UserProfile.starter(username));
        footerStatus.setText("ПРОФИЛЬ НЕДОСТУПЕН");
        setOpacity(1.0);
    }

    /** Scales the design-size card without changing its top-left anchor. */
    public void applyViewportScale(double scale) {
        double safeScale = Math.max(0.78, Math.min(1.12, scale));
        setScaleX(safeScale);
        setScaleY(safeScale);
    }

    public double getScaledDesignWidth() {
        return DESIGN_WIDTH * getScaleX();
    }

    public double getScaledDesignHeight() {
        return DESIGN_HEIGHT * getScaleY();
    }

    private StackPane createAvatar() {
        Circle outerGlow = new Circle(48);
        outerGlow.getStyleClass().add("profile-avatar__outer");

        Circle inner = new Circle(39);
        inner.getStyleClass().add("profile-avatar__inner");

        Circle head = new Circle(0, -9, 11, Color.rgb(19, 66, 103, 0.95));
        Arc shoulders = new Arc(0, 22, 25, 20, 0, 180);
        shoulders.setType(ArcType.ROUND);
        shoulders.setFill(Color.rgb(14, 54, 88, 0.98));
        shoulders.setStroke(Color.rgb(74, 205, 255, 0.58));

        Polygon collar = new Polygon(
                -15.0, 15.0,
                0.0, 29.0,
                15.0, 15.0,
                8.0, 10.0,
                0.0, 19.0,
                -8.0, 10.0
        );
        collar.setFill(Color.rgb(18, 99, 145, 0.88));

        Group silhouette = new Group(shoulders, head, collar);
        StackPane avatar = new StackPane(outerGlow, inner, silhouette);
        avatar.setMinSize(100, 100);
        avatar.setPrefSize(100, 100);
        avatar.setMaxSize(100, 100);
        return avatar;
    }

    private void consumeSecondaryClicks() {
        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
    }
}
