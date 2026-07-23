package org.example;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds motion to the existing data-driven CircularMenuPane without taking over
 * its navigation or database logic.
 */
public final class OrbitAnimationSupport {
    private static final Interpolator ORBIT_INTERPOLATOR =
            Interpolator.SPLINE(0.12, 0.82, 0.22, 1.0);

    private final CircularMenuPane pane;
    private final List<Animation> ambientAnimations = new ArrayList<>();
    private final Map<Node, Animation> entryAnimations = new IdentityHashMap<>();
    private final ListChangeListener<Node> childrenListener = this::onChildrenChanged;
    private boolean ambientStarted;

    private OrbitAnimationSupport(CircularMenuPane pane) {
        this.pane = pane;
    }

    public static OrbitAnimationSupport install(CircularMenuPane pane) {
        OrbitAnimationSupport support = new OrbitAnimationSupport(pane);
        if (!AnimationPreferences.isEnabled()) {
            return support;
        }

        pane.getChildren().addListener(support.childrenListener);
        pane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                support.stopAmbientAnimations();
            } else {
                Platform.runLater(support::startAmbientAnimations);
            }
        });
        Platform.runLater(support::startAmbientAnimations);
        return support;
    }

    public void dispose() {
        pane.getChildren().removeListener(childrenListener);
        stopAmbientAnimations();
        entryAnimations.values().forEach(Animation::stop);
        entryAnimations.clear();
    }

    private void onChildrenChanged(ListChangeListener.Change<? extends Node> change) {
        int staggerIndex = 0;
        boolean orbitContentChanged = false;
        while (change.next()) {
            if (change.wasRemoved()) {
                for (Node removed : change.getRemoved()) {
                    Animation running = entryAnimations.remove(removed);
                    if (running != null) {
                        running.stop();
                    }
                    if (removed instanceof OrbitItemView) {
                        orbitContentChanged = true;
                    }
                }
            }
            if (change.wasAdded()) {
                for (Node added : change.getAddedSubList()) {
                    if (added instanceof OrbitItemView) {
                        animateOrbitItem(added, staggerIndex++);
                        orbitContentChanged = true;
                    }
                }
            }
        }

        if (orbitContentChanged) {
            animatePaneRefresh();
        }
    }

    private void animateOrbitItem(Node item, int staggerIndex) {
        Platform.runLater(() -> {
            if (item.getParent() != pane || !item.isVisible()) {
                return;
            }

            pane.applyCss();
            pane.layout();

            Bounds bounds = item.getBoundsInParent();
            double itemCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
            double itemCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;
            double fromX = pane.getWidth() / 2.0 - itemCenterX;
            double fromY = pane.getHeight() * 0.46 - itemCenterY;

            item.setOpacity(0.0);
            item.setScaleX(0.24);
            item.setScaleY(0.24);
            item.setTranslateX(fromX);
            item.setTranslateY(fromY);

            PauseTransition delay = new PauseTransition(
                    AnimationPreferences.duration(Math.min(210, staggerIndex * 34.0))
            );

            FadeTransition fade = new FadeTransition(
                    AnimationPreferences.duration(235), item
            );
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(
                    AnimationPreferences.duration(310), item
            );
            scale.setFromX(0.24);
            scale.setFromY(0.24);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(ORBIT_INTERPOLATOR);

            TranslateTransition translate = new TranslateTransition(
                    AnimationPreferences.duration(330), item
            );
            translate.setFromX(fromX);
            translate.setFromY(fromY);
            translate.setToX(0.0);
            translate.setToY(0.0);
            translate.setInterpolator(ORBIT_INTERPOLATOR);

            ParallelTransition enter = new ParallelTransition(fade, scale, translate);
            SequentialTransition sequence = new SequentialTransition(delay, enter);
            sequence.setOnFinished(event -> {
                entryAnimations.remove(item);
                item.setOpacity(1.0);
                item.setScaleX(1.0);
                item.setScaleY(1.0);
                item.setTranslateX(0.0);
                item.setTranslateY(0.0);
            });

            Animation previous = entryAnimations.put(item, sequence);
            if (previous != null) {
                previous.stop();
            }
            sequence.play();
        });
    }

    private void animatePaneRefresh() {
        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(
                    AnimationPreferences.duration(180), pane
            );
            fade.setFromValue(Math.min(0.90, pane.getOpacity()));
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        });
    }

    private void startAmbientAnimations() {
        if (ambientStarted || pane.getScene() == null || !AnimationPreferences.isEnabled()) {
            return;
        }
        ambientStarted = true;
        pane.applyCss();

        animateDashOffset(".orbit-ring--outer", 0.0, -96.0, 13.0);
        animateDashOffset(".orbit-ring--middle", 0.0, 130.0, 20.0);
        animateDashOffset(".orbit-ring--core", 0.0, -72.0, 17.0);
        animatePulse(".command-crystal__glow", 0.68, 1.0, 1.0, 1.035, 2.6);
        animatePulse(".orbit-backdrop", 0.72, 0.98, 1.0, 1.018, 4.2);
    }

    private void animateDashOffset(
            String selector,
            double from,
            double to,
            double seconds
    ) {
        Node node = pane.lookup(selector);
        if (!(node instanceof Shape shape)) {
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shape.strokeDashOffsetProperty(), from)),
                new KeyFrame(AnimationPreferences.duration(seconds * 1000.0),
                        new KeyValue(shape.strokeDashOffsetProperty(), to,
                                Interpolator.LINEAR))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        ambientAnimations.add(timeline);
    }

    private void animatePulse(
            String selector,
            double minOpacity,
            double maxOpacity,
            double minScale,
            double maxScale,
            double seconds
    ) {
        Node node = pane.lookup(selector);
        if (node == null) {
            return;
        }

        FadeTransition fade = new FadeTransition(
                AnimationPreferences.duration(seconds * 1000.0), node
        );
        fade.setFromValue(minOpacity);
        fade.setToValue(maxOpacity);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(
                AnimationPreferences.duration(seconds * 1000.0), node
        );
        scale.setFromX(minScale);
        scale.setFromY(minScale);
        scale.setToX(maxScale);
        scale.setToY(maxScale);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pulse = new ParallelTransition(fade, scale);
        pulse.play();
        ambientAnimations.add(pulse);
    }

    private void stopAmbientAnimations() {
        ambientAnimations.forEach(Animation::stop);
        ambientAnimations.clear();
        ambientStarted = false;
    }
}
