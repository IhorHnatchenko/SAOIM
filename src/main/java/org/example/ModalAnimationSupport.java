package org.example;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;

/** Adds a lightweight appearance animation to the in-scene editor layer. */
public final class ModalAnimationSupport {
    private ModalAnimationSupport() {
    }

    public static void install(Parent profileRoot) {
        if (!AnimationPreferences.isEnabled()) {
            return;
        }

        profileRoot.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> bindDialogLayer(profileRoot));
            }
        });
        Platform.runLater(() -> bindDialogLayer(profileRoot));
    }

    private static void bindDialogLayer(Parent root) {
        root.applyCss();
        Node layer = root.lookup(".category-dialog-layer");
        if (layer == null || Boolean.TRUE.equals(
                layer.getProperties().get("saoim.modal-animation-bound")
        )) {
            return;
        }

        layer.getProperties().put("saoim.modal-animation-bound", Boolean.TRUE);
        layer.visibleProperty().addListener((observable, oldValue, visible) -> {
            if (visible) {
                Platform.runLater(() -> animateOpen(root));
            }
        });

        if (layer.isVisible()) {
            animateOpen(root);
        }
    }

    private static void animateOpen(Parent root) {
        Node dimmer = root.lookup(".category-dialog-dimmer");
        Node card = root.lookup(".category-dialog-card");
        if (card == null) {
            return;
        }

        if (dimmer != null) {
            FadeTransition dimmerFade = new FadeTransition(
                    AnimationPreferences.duration(150), dimmer
            );
            dimmerFade.setFromValue(0.0);
            dimmerFade.setToValue(1.0);
            dimmerFade.play();
        }

        card.setOpacity(0.0);
        card.setScaleX(0.92);
        card.setScaleY(0.92);
        card.setTranslateY(18.0);

        FadeTransition fade = new FadeTransition(
                AnimationPreferences.duration(210), card
        );
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(
                AnimationPreferences.duration(245), card
        );
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.SPLINE(0.18, 0.84, 0.30, 1.0));

        TranslateTransition translate = new TranslateTransition(
                AnimationPreferences.duration(245), card
        );
        translate.setFromY(18.0);
        translate.setToY(0.0);
        translate.setInterpolator(Interpolator.SPLINE(0.18, 0.84, 0.30, 1.0));

        new ParallelTransition(fade, scale, translate).play();
    }
}
