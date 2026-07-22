package org.example;

import javafx.concurrent.Task;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Profile screen: profile card and the interactive circular menu. */
public final class ProfileView extends Pane {
    private static final double MIN_MARGIN = 18;
    private static final double DEFAULT_MARGIN = 30;

    private static final ExecutorService PROFILE_EXECUTOR =
            Executors.newSingleThreadExecutor(new DaemonThreadFactory());

    private final ProfileCard profileCard = new ProfileCard();
    private final CircularMenuPane orbitPane =
            new CircularMenuPane(DemoOrbitData.createRootEntries());
    private final ProfileRepository profileRepository = new ProfileRepository();

    private UserSession session = UserSession.guest();
    private UserProfile profile = UserProfile.starter("Guest");
    private Task<UserProfile> activeProfileTask;
    private long loadGeneration;

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

    /** Compatibility method for older callers. */
    public void setUsername(String username) {
        setSession(UserSession.guest(username));
    }

    public void setSession(UserSession session) {
        this.session = session == null ? UserSession.guest() : session;

        if (!this.session.isAuthenticated()) {
            cancelProfileLoad();
            setProfile(UserProfile.starter(this.session.getUsername()));
        } else if (profile.getAccountId() != this.session.getAccountId()) {
            setProfile(UserProfile.starter(
                    this.session.getAccountId(),
                    this.session.getUsername()
            ));
        }
    }

    public UserSession getSession() {
        return session;
    }

    /**
     * Loads the profile outside the JavaFX Application Thread. Task callbacks
     * are delivered on the JavaFX thread by the Task API.
     */
    public void loadProfileAsync(UserSession requestedSession) {
        setSession(requestedSession);

        if (!session.isAuthenticated()) {
            return;
        }

        cancelProfileLoad();
        long generation = ++loadGeneration;
        long accountId = session.getAccountId();
        String username = session.getUsername();

        profileCard.showLoading(username);

        Task<UserProfile> task = new Task<>() {
            @Override
            protected UserProfile call() throws Exception {
                return profileRepository.findByAccountId(accountId, username);
            }
        };

        task.setOnSucceeded(event -> {
            if (generation != loadGeneration || accountId != session.getAccountId()) {
                return;
            }
            setProfile(task.getValue());
            activeProfileTask = null;
            System.out.println("[Profile] Профиль загружен для accountId=" + accountId);
        });

        task.setOnFailed(event -> {
            if (generation != loadGeneration || accountId != session.getAccountId()) {
                return;
            }
            Throwable exception = task.getException();
            profile = UserProfile.starter(accountId, username);
            profileCard.showLoadError(username);
            activeProfileTask = null;
            System.err.println(
                    "[Profile] Не удалось загрузить профиль accountId=" + accountId + ": " +
                            (exception == null ? "неизвестная ошибка" : exception.getMessage())
            );
        });

        task.setOnCancelled(event -> {
            if (activeProfileTask == task) {
                activeProfileTask = null;
            }
        });

        activeProfileTask = task;
        PROFILE_EXECUTOR.execute(task);
    }

    public void cancelProfileLoad() {
        loadGeneration++;
        if (activeProfileTask != null) {
            activeProfileTask.cancel(true);
            activeProfileTask = null;
        }
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile == null
                ? UserProfile.starter(session.getAccountId(), session.getUsername())
                : profile;
        profileCard.setProfile(this.profile);
    }

    public UserProfile getProfile() {
        return profile;
    }

    /** @return true when Esc was consumed by nested orbit navigation. */
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

        double scaleXCompensation =
                (cardWidth - profileCard.getPrefWidth()) / 2.0;
        double scaleYCompensation =
                (cardHeight - profileCard.getPrefHeight()) / 2.0;

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

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "saoim-profile-loader");
            thread.setDaemon(true);
            return thread;
        }
    }
}
