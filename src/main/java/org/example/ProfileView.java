package org.example;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.net.URL;

/**
 * Profile screen: profile card and the interactive circular menu.
 */
public final class ProfileView extends Pane {

    private static final double MIN_MARGIN = 18;
    private static final double DEFAULT_MARGIN = 30;

    private final ProfileCard profileCard = new ProfileCard();
    private final CircularMenuPane orbitPane =
            new CircularMenuPane(DemoOrbitData.createRootEntries());

    private UserProfile profile = UserProfile.starter("Guest");

    public ProfileView() {
        getStyleClass().add("profile-view");
        setPickOnBounds(false);

        URL stylesheet = ProfileView.class.getResource("/org/example/profile-view.css");
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

    /**
     * @return true when Esc was consumed by nested orbit navigation.
     */
    public boolean handleEscape() {
        return orbitPane.handleEscape();
    }

    public void resetOrbitNavigation() {
        orbitPane.resetNavigation();
    }

    public CircularMenuPane getOrbitPane() {
        return orbitPane;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double viewportScale = clamp(
                Math.min(width / 1920.0, height / 1080.0),
                0.78,
                1.10
        );
        profileCard.applyViewportScale(viewportScale);

        double margin = clamp(
                Math.min(width, height) * 0.028,
                MIN_MARGIN,
                DEFAULT_MARGIN
        );
        double cardWidth = profileCard.getScaledDesignWidth();
        double cardHeight = profileCard.getScaledDesignHeight();

        // Scaling occurs around the node center; compensate to keep the visual
        // profile card anchored to the upper-left corner.
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
