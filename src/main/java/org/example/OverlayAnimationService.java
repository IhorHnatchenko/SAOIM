package org.example;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * Coordinates transitions between HIDDEN, STANDARD_MENU and PROFILE.
 *
 * The service always finishes by applying an explicit visual state. If JavaFX
 * cannot start an animation for any reason, it falls back to an immediate
 * transition instead of leaving the logical state and visible nodes out of
 * sync.
 */
public final class OverlayAnimationService {
    private static final Interpolator ENTER_INTERPOLATOR =
            Interpolator.SPLINE(0.16, 0.84, 0.30, 1.0);
    private static final Interpolator EXIT_INTERPOLATOR =
            Interpolator.SPLINE(0.55, 0.00, 0.85, 0.45);

    private Animation activeAnimation;
    private PauseTransition activeWatchdog;
    private long animationGeneration;

    public void showStandardMenu(
            MenuState previousState,
            Stage stage,
            Node standardMenu,
            Node profileView
    ) {
        stopActiveAnimation();

        try {
            if (!AnimationPreferences.isEnabled()) {
                finishShowing(stage, standardMenu, profileView);
                return;
            }

            if (previousState == MenuState.PROFILE) {
                switchViews(
                        stage,
                        profileView,
                        standardMenu,
                        -34.0,
                        26.0
                );
                return;
            }

            hideImmediately(profileView);
            showImmediately(standardMenu);
            playAnimation(
                    stage,
                    entrance(standardMenu, -28.0, 0.88, 235.0),
                    standardMenu,
                    () -> finishShowing(stage, standardMenu, profileView)
            );
        } catch (RuntimeException exception) {
            reportAnimationFailure("STANDARD_MENU", exception);
            finishShowing(stage, standardMenu, profileView);
        }
    }

    public void showProfile(
            MenuState previousState,
            Stage stage,
            Node standardMenu,
            Node profileView
    ) {
        stopActiveAnimation();

        try {
            if (!AnimationPreferences.isEnabled()) {
                finishShowing(stage, profileView, standardMenu);
                return;
            }

            if (previousState == MenuState.STANDARD_MENU) {
                switchViews(
                        stage,
                        standardMenu,
                        profileView,
                        -30.0,
                        0.0
                );
                return;
            }

            hideImmediately(standardMenu);
            showImmediately(profileView);
            playAnimation(
                    stage,
                    profileEntrance(profileView),
                    profileView,
                    () -> finishShowing(stage, profileView, standardMenu)
            );
        } catch (RuntimeException exception) {
            reportAnimationFailure("PROFILE", exception);
            finishShowing(stage, profileView, standardMenu);
        }
    }

    public void hide(
            MenuState previousState,
            Stage stage,
            Node standardMenu,
            Node profileView,
            Runnable afterHidden
    ) {
        stopActiveAnimation();

        Node activeNode = previousState == MenuState.PROFILE
                ? profileView
                : standardMenu;
        boolean[] completed = {false};

        Runnable finish = () -> {
            if (completed[0]) {
                return;
            }
            completed[0] = true;
            stopWatchdog();

            hideImmediately(standardMenu);
            hideImmediately(profileView);
            if (stage.isShowing()) {
                stage.hide();
            }
            runSafely(afterHidden);
        };

        try {
            if (!AnimationPreferences.isEnabled() || !stage.isShowing()) {
                finish.run();
                return;
            }

            showImmediately(activeNode);
            playAnimation(
                    stage,
                    exit(activeNode, -18.0, 0.94, 165.0),
                    activeNode,
                    finish
            );

            // Defensive fallback: even if a JavaFX transition is interrupted
            // by a focus/window event, the full-screen overlay must not remain
            // visible while the logical state is already HIDDEN.
            armWatchdog(AnimationPreferences.duration(650.0), finish);
        } catch (RuntimeException exception) {
            reportAnimationFailure("HIDDEN", exception);
            finish.run();
        }
    }

    public boolean isAnimating() {
        return activeAnimation != null
                && activeAnimation.getStatus() == Animation.Status.RUNNING;
    }

    public void stopAndReset(Node standardMenu, Node profileView) {
        stopActiveAnimation();
        resetNode(standardMenu);
        resetNode(profileView);
    }

    /** Immediately repairs the visible state without running an animation. */
    public void forceStandardMenu(Stage stage, Node standardMenu, Node profileView) {
        stopActiveAnimation();
        finishShowing(stage, standardMenu, profileView);
    }

    /** Immediately repairs the visible state without running an animation. */
    public void forceProfile(Stage stage, Node standardMenu, Node profileView) {
        stopActiveAnimation();
        finishShowing(stage, profileView, standardMenu);
    }

    /** Immediately hides both overlay views and the Stage. */
    public void forceHidden(Stage stage, Node standardMenu, Node profileView) {
        stopActiveAnimation();
        hideImmediately(standardMenu);
        hideImmediately(profileView);
        if (stage != null && stage.isShowing()) {
            stage.hide();
        }
    }

    private void switchViews(
            Stage stage,
            Node outgoing,
            Node incoming,
            double outgoingX,
            double incomingX
    ) {
        showImmediately(outgoing);
        showImmediately(incoming);

        incoming.setOpacity(0.0);
        incoming.setScaleX(0.90);
        incoming.setScaleY(0.90);
        incoming.setTranslateX(incomingX);

        Animation exit = exit(outgoing, outgoingX, 0.94, 125.0);
        Animation enter = entrance(incoming, incomingX, 0.90, 265.0);
        SequentialTransition sequence = new SequentialTransition(exit, enter);

        playAnimation(
                stage,
                sequence,
                incoming,
                () -> finishShowing(stage, incoming, outgoing),
                outgoing
        );
    }

    private Animation profileEntrance(Node node) {
        node.setOpacity(0.0);
        node.setScaleX(0.92);
        node.setScaleY(0.92);
        node.setTranslateY(12.0);

        FadeTransition fade = new FadeTransition(AnimationPreferences.duration(300), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(AnimationPreferences.duration(330), node);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(ENTER_INTERPOLATOR);

        TranslateTransition translate = new TranslateTransition(
                AnimationPreferences.duration(330), node
        );
        translate.setFromY(12.0);
        translate.setToY(0.0);
        translate.setInterpolator(ENTER_INTERPOLATOR);

        return new ParallelTransition(fade, scale, translate);
    }

    private Animation entrance(Node node, double fromX, double fromScale, double millis) {
        node.setOpacity(0.0);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);
        node.setTranslateX(fromX);

        FadeTransition fade = new FadeTransition(AnimationPreferences.duration(millis), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(
                AnimationPreferences.duration(millis + 35), node
        );
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(ENTER_INTERPOLATOR);

        TranslateTransition translate = new TranslateTransition(
                AnimationPreferences.duration(millis + 35), node
        );
        translate.setFromX(fromX);
        translate.setToX(0.0);
        translate.setInterpolator(ENTER_INTERPOLATOR);

        return new ParallelTransition(fade, scale, translate);
    }

    private Animation exit(Node node, double toX, double toScale, double millis) {
        FadeTransition fade = new FadeTransition(AnimationPreferences.duration(millis), node);
        fade.setFromValue(Math.max(0.0, node.getOpacity()));
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scale = new ScaleTransition(AnimationPreferences.duration(millis), node);
        scale.setToX(toScale);
        scale.setToY(toScale);
        scale.setInterpolator(EXIT_INTERPOLATOR);

        TranslateTransition translate = new TranslateTransition(
                AnimationPreferences.duration(millis), node
        );
        translate.setToX(toX);
        translate.setInterpolator(EXIT_INTERPOLATOR);

        return new ParallelTransition(fade, scale, translate);
    }

    private void playAnimation(
            Stage stage,
            Animation animation,
            Node primaryAnimatedNode,
            Runnable completion,
            Node... otherAnimatedNodes
    ) {
        long generation = ++animationGeneration;
        activeAnimation = animation;

        enableCache(primaryAnimatedNode);
        for (Node node : otherAnimatedNodes) {
            enableCache(node);
        }

        animation.setOnFinished(event -> {
            if (generation != animationGeneration) {
                return;
            }

            activeAnimation = null;
            disableCache(primaryAnimatedNode);
            for (Node node : otherAnimatedNodes) {
                disableCache(node);
            }

            runSafely(completion);
            requestStageFocus(stage);
        });

        animation.play();
    }

    private void finishShowing(Stage stage, Node visibleNode, Node hiddenNode) {
        hideImmediately(hiddenNode);
        showImmediately(visibleNode);
        requestStageFocus(stage);
    }

    private void stopActiveAnimation() {
        animationGeneration++;
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
        stopWatchdog();
    }

    private void armWatchdog(javafx.util.Duration timeout, Runnable fallback) {
        stopWatchdog();
        activeWatchdog = new PauseTransition(timeout);
        activeWatchdog.setOnFinished(event -> {
            activeWatchdog = null;
            runSafely(fallback);
        });
        activeWatchdog.play();
    }

    private void stopWatchdog() {
        if (activeWatchdog != null) {
            activeWatchdog.stop();
            activeWatchdog = null;
        }
    }

    private void showImmediately(Node node) {
        node.setManaged(true);
        node.setVisible(true);
        resetNode(node);
    }

    private void hideImmediately(Node node) {
        resetNode(node);
        node.setVisible(false);
        node.setManaged(false);
        disableCache(node);
    }

    private void resetNode(Node node) {
        node.setOpacity(1.0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        node.setTranslateX(0.0);
        node.setTranslateY(0.0);
        node.setRotate(0.0);
    }

    private void enableCache(Node node) {
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);
    }

    private void disableCache(Node node) {
        node.setCache(false);
        node.setCacheHint(CacheHint.DEFAULT);
    }

    private void requestStageFocus(Stage stage) {
        if (stage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.toFront();
                stage.requestFocus();
            }
        });
    }

    private void reportAnimationFailure(String targetState, RuntimeException exception) {
        System.err.println(
                "[Animation] Не удалось выполнить переход к " + targetState
                        + ": " + exception.getMessage()
        );
        exception.printStackTrace();
    }

    private void runSafely(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }
}
