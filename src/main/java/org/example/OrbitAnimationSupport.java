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
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight orbit motion that batches CSS/layout work. The previous version
 * forced a complete applyCss/layout pass once for every added item and also
 * ran five infinite animations. That made ordinary profile operations visibly
 * stutter on many Windows/JavaFX configurations.
 */
public final class OrbitAnimationSupport {
    private static final Interpolator ORBIT_INTERPOLATOR =
            Interpolator.SPLINE(0.18, 0.82, 0.28, 1.0);

    private final CircularMenuPane pane;
    private final List<Animation> ambientAnimations = new ArrayList<>();
    private final Map<Node, Animation> entryAnimations = new IdentityHashMap<>();
    private final Set<Node> pendingItems = Collections.newSetFromMap(
            new IdentityHashMap<>()
    );
    private final ListChangeListener<Node> childrenListener = this::onChildrenChanged;

    private boolean entryBatchScheduled;
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
                Platform.runLater(support::configureStaticCaches);
                if (AnimationPreferences.isAmbientEnabled()) {
                    Platform.runLater(support::startAmbientAnimations);
                }
            }
        });

        Platform.runLater(support::configureStaticCaches);
        if (AnimationPreferences.isAmbientEnabled()) {
            Platform.runLater(support::startAmbientAnimations);
        }
        return support;
    }

    public void dispose() {
        pane.getChildren().removeListener(childrenListener);
        stopAmbientAnimations();
        entryAnimations.forEach((node, animation) -> {
            animation.stop();
            resetItem(node);
        });
        entryAnimations.clear();
        pendingItems.clear();
    }

    private void onChildrenChanged(ListChangeListener.Change<? extends Node> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                for (Node removed : change.getRemoved()) {
                    pendingItems.remove(removed);
                    Animation running = entryAnimations.remove(removed);
                    if (running != null) {
                        running.stop();
                    }
                    resetItem(removed);
                }
            }

            if (change.wasAdded()) {
                for (Node added : change.getAddedSubList()) {
                    if (added instanceof OrbitItemView) {
                        pendingItems.add(added);
                    }
                }
            }
        }

        scheduleEntryBatch();
    }

    /** One JavaFX pulse and one layout pass for the complete refreshed page. */
    private void scheduleEntryBatch() {
        if (pendingItems.isEmpty() || entryBatchScheduled) {
            return;
        }

        entryBatchScheduled = true;
        Platform.runLater(() -> {
            entryBatchScheduled = false;

            List<Node> items = pendingItems.stream()
                    .filter(node -> node.getParent() == pane && node.isVisible())
                    .toList();
            pendingItems.clear();

            if (items.isEmpty()) {
                return;
            }

            pane.applyCss();
            pane.layout();

            double centerX = pane.getWidth() / 2.0;
            double centerY = pane.getHeight() * 0.46;
            for (int index = 0; index < items.size(); index++) {
                animateOrbitItem(items.get(index), index, centerX, centerY);
            }
        });
    }

    private void animateOrbitItem(
            Node item,
            int staggerIndex,
            double centerX,
            double centerY
    ) {
        Bounds bounds = item.getBoundsInParent();
        double itemCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
        double itemCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;

        // A shorter travel and milder scale retain the visual effect without
        // repeatedly rasterising large shadows over the whole orbit.
        double fromX = (centerX - itemCenterX) * 0.32;
        double fromY = (centerY - itemCenterY) * 0.32;

        Animation previous = entryAnimations.remove(item);
        if (previous != null) {
            previous.stop();
        }

        item.setCache(true);
        item.setCacheHint(CacheHint.SPEED);
        item.setOpacity(0.0);
        item.setScaleX(0.86);
        item.setScaleY(0.86);
        item.setTranslateX(fromX);
        item.setTranslateY(fromY);

        PauseTransition delay = new PauseTransition(
                AnimationPreferences.duration(Math.min(84.0, staggerIndex * 12.0))
        );

        FadeTransition fade = new FadeTransition(
                AnimationPreferences.duration(150.0), item
        );
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(
                AnimationPreferences.duration(205.0), item
        );
        scale.setFromX(0.86);
        scale.setFromY(0.86);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(ORBIT_INTERPOLATOR);

        TranslateTransition translate = new TranslateTransition(
                AnimationPreferences.duration(215.0), item
        );
        translate.setFromX(fromX);
        translate.setFromY(fromY);
        translate.setToX(0.0);
        translate.setToY(0.0);
        translate.setInterpolator(ORBIT_INTERPOLATOR);

        SequentialTransition sequence = new SequentialTransition(
                delay,
                new ParallelTransition(fade, scale, translate)
        );
        sequence.setOnFinished(event -> {
            entryAnimations.remove(item);
            resetItem(item);
        });

        entryAnimations.put(item, sequence);
        sequence.play();
    }

    /** Cache static geometry after CSS has created all selector nodes. */
    private void configureStaticCaches() {
        if (pane.getScene() == null) {
            return;
        }
        pane.applyCss();

        cacheStaticNode(".orbit-backdrop");
        cacheStaticNode(".orbit-ring--outer");
        cacheStaticNode(".orbit-ring--middle");
        cacheStaticNode(".orbit-ring--core");
        cacheStaticNode(".command-crystal__glow");
    }

    private void cacheStaticNode(String selector) {
        Node node = pane.lookup(selector);
        if (node != null) {
            node.setCache(true);
            node.setCacheHint(CacheHint.SPEED);
        }
    }

    private void resetItem(Node item) {
        item.setOpacity(1.0);
        item.setScaleX(1.0);
        item.setScaleY(1.0);
        item.setTranslateX(0.0);
        item.setTranslateY(0.0);
        item.setCache(false);
        item.setCacheHint(CacheHint.DEFAULT);
    }

    private void startAmbientAnimations() {
        if (ambientStarted
                || pane.getScene() == null
                || !AnimationPreferences.isAmbientEnabled()) {
            return;
        }
        ambientStarted = true;
        pane.applyCss();

        // Optional reduced ambient mode: one ring and one very slow glow,
        // instead of three rotating rings plus two simultaneous pulses.
        animateDashOffset(".orbit-ring--outer", 0.0, -96.0, 18.0);
        animateGlow(".command-crystal__glow", 0.78, 1.0, 4.2);
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
        shape.setCache(false);

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

    private void animateGlow(
            String selector,
            double minOpacity,
            double maxOpacity,
            double seconds
    ) {
        Node node = pane.lookup(selector);
        if (node == null) {
            return;
        }
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);

        FadeTransition fade = new FadeTransition(
                AnimationPreferences.duration(seconds * 1000.0), node
        );
        fade.setFromValue(minOpacity);
        fade.setToValue(maxOpacity);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.play();
        ambientAnimations.add(fade);
    }

    private void stopAmbientAnimations() {
        ambientAnimations.forEach(Animation::stop);
        ambientAnimations.clear();
        ambientStarted = false;
    }
}
