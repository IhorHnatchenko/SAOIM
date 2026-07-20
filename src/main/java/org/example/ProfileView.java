package org.example;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.net.URL;

/**
 * Step 3 profile screen: profile card, command crystal and five static items.
 */
public final class ProfileView extends Pane {

    private static final double MIN_MARGIN = 18;
    private static final double DEFAULT_MARGIN = 30;

    private final ProfileCard profileCard = new ProfileCard();
    private final StaticOrbitPane orbitPane = new StaticOrbitPane();
    private UserProfile profile = UserProfile.starter("Guest");

    public ProfileView() {
        getStyleClass().add("profile-view");
        setPickOnBounds(false);

        URL stylesheet = ProfileView.class.getResource("profile-view.css");
        if (stylesheet != null) {
            getStylesheets().add(stylesheet.toExternalForm());
        } else {
            System.err.println("[Profile] Не найден stylesheet profile-view.css");
        }

        getChildren().addAll(profileCard, orbitPane);
        profileCard.setProfile(profile);

        consumeSecondaryClicks(profileCard);
    }

    public void setUsername(String username) {
        setProfile(profile.withNickname(username));
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile == null ? UserProfile.starter("Guest") : profile;
        profileCard.setProfile(this.profile);
    }

    public UserProfile getProfile() {
        return profile;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double viewportScale = clamp(Math.min(width / 1920.0, height / 1080.0), 0.78, 1.10);
        profileCard.applyViewportScale(viewportScale);

        double margin = clamp(Math.min(width, height) * 0.028, MIN_MARGIN, DEFAULT_MARGIN);
        double cardWidth = profileCard.getScaledDesignWidth();
        double cardHeight = profileCard.getScaledDesignHeight();

        // Because scaling is performed around the node center, compensate so the
        // visual card remains anchored to the upper-left margin.
        double scaleXCompensation = (cardWidth - profileCard.getPrefWidth()) / 2.0;
        double scaleYCompensation = (cardHeight - profileCard.getPrefHeight()) / 2.0;
        profileCard.resizeRelocate(
                margin + scaleXCompensation,
                margin + scaleYCompensation,
                profileCard.getPrefWidth(),
                profileCard.getPrefHeight()
        );

        double orbitLeft = Math.max(margin, margin + cardWidth * 0.82);
        double orbitTop = margin * 0.35;
        double orbitWidth = Math.max(320, width - orbitLeft - margin);
        double orbitHeight = Math.max(320, height - orbitTop - margin * 0.35);

        orbitPane.resizeRelocate(orbitLeft, orbitTop, orbitWidth, orbitHeight);
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
