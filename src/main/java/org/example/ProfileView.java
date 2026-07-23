package org.example;

import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Profile screen: profile card, mixed orbit data and modal editor layer. */
public final class ProfileView extends Pane {
    private static final double MIN_MARGIN = 18;
    private static final double DEFAULT_MARGIN = 30;
    private static final ExecutorService DATA_EXECUTOR = Executors.newFixedThreadPool(
            2,
            new DaemonThreadFactory()
    );

    private final ProfileCard profileCard = new ProfileCard();
    private final CircularMenuPane orbitPane = new CircularMenuPane(List.of());
    private final CategoryDialogLayer dialogLayer = new CategoryDialogLayer();
    private final ProfileRepository profileRepository = new ProfileRepository();
    private final CategoryService categoryService = new CategoryService();
    private final ShortcutService shortcutService = new ShortcutService();
    private final AppLauncher appLauncher = new AppLauncher();
    private UndoManager undoManager = new UndoManager();

    private UserSession session = UserSession.guest();
    private UserProfile profile = UserProfile.starter("Guest");
    private CategorySnapshot categorySnapshot = emptySnapshot(-1);
    private Task<UserProfile> activeProfileTask;
    private Task<CategorySnapshot> activeDataTask;
    private Task<LaunchResult> activeLaunchTask;
    private long profileLoadGeneration;
    private long dataLoadGeneration;
    private long launchGeneration;
    private Runnable onShortcutLaunched = () -> { };

    public ProfileView() {
        getStyleClass().add("profile-view");
        setPickOnBounds(false);
        loadStylesheet("/org/example/profile-view.css");
        loadStylesheet("/org/example/category-view.css");
        loadStylesheet("/org/example/shortcut-view.css");

        getChildren().addAll(profileCard, orbitPane, dialogLayer);
        profileCard.setProfile(profile);
        consumeSecondaryClicks(profileCard);
        orbitPane.setShortcutResolver(id -> categorySnapshot.findShortcut(id));
        configureOrbitActions();
        updateUndoState();
    }

    /** Compatibility method for older callers. */
    public void setUsername(String username) {
        setSession(UserSession.guest(username));
    }

    public void setSession(UserSession session) {
        UserSession next = session == null ? UserSession.guest() : session;
        boolean accountChanged = this.session.getAccountId() != next.getAccountId();
        this.session = next;

        if (accountChanged || !this.session.isAuthenticated()) {
            undoManager = new UndoManager();
            updateUndoState();
        }

        if (!this.session.isAuthenticated()) {
            cancelProfileLoad();
            setProfile(UserProfile.starter(this.session.getUsername()));
            categorySnapshot = emptySnapshot(-1);
            orbitPane.setCategoryData(List.of(), List.of(), List.of(), true);
        } else if (accountChanged) {
            cancelProfileLoad();
            setProfile(UserProfile.starter(
                    this.session.getAccountId(),
                    this.session.getUsername()
            ));
            categorySnapshot = emptySnapshot(this.session.getAccountId());
            orbitPane.setCategoryData(List.of(), List.of(), List.of(), true);
        }
    }

    public UserSession getSession() {
        return session;
    }

    public void setOnShortcutLaunched(Runnable onShortcutLaunched) {
        this.onShortcutLaunched = onShortcutLaunched == null
                ? () -> { }
                : onShortcutLaunched;
    }

    /** Loads profile, categories and shortcuts outside JavaFX Application Thread. */
    public void loadProfileAsync(UserSession requestedSession) {
        setSession(requestedSession);
        if (!session.isAuthenticated()) {
            return;
        }
        loadProfileCardAsync();
        loadCategoriesAsync(false);
    }

    private void loadProfileCardAsync() {
        cancelProfileTask();
        long generation = ++profileLoadGeneration;
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
            if (generation != profileLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            setProfile(task.getValue());
            activeProfileTask = null;
            System.out.println("[Profile] Профиль загружен для accountId=" + accountId);
        });
        task.setOnFailed(event -> {
            if (generation != profileLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            Throwable exception = task.getException();
            profile = UserProfile.starter(accountId, username);
            profileCard.showLoadError(username);
            activeProfileTask = null;
            System.err.println(
                    "[Profile] Не удалось загрузить профиль accountId="
                            + accountId + ": " + messageFrom(exception)
            );
        });
        task.setOnCancelled(event -> {
            if (activeProfileTask == task) {
                activeProfileTask = null;
            }
        });
        activeProfileTask = task;
        DATA_EXECUTOR.execute(task);
    }

    public void loadCategoriesAsync(boolean preserveNavigation) {
        if (!session.isAuthenticated()) {
            return;
        }
        cancelDataTask();
        long generation = ++dataLoadGeneration;
        long accountId = session.getAccountId();
        List<Long> path = preserveNavigation
                ? orbitPane.getNavigationPathCategoryIds()
                : List.of();
        boolean showAllRoots = preserveNavigation
                && orbitPane.isShowingAllRootCategories();

        orbitPane.setBusy(true, "Загрузка категорий и ярлыков…");
        Task<CategorySnapshot> task = new Task<>() {
            @Override
            protected CategorySnapshot call() throws Exception {
                return categoryService.loadSnapshot(accountId);
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            applyCategorySnapshot(task.getValue(), path, showAllRoots);
            orbitPane.setBusy(false, "");
            System.out.println(
                    "[Orbit] Загружено: категорий="
                            + categorySnapshot.getCategories().size()
                            + ", ярлыков=" + categorySnapshot.getShortcuts().size()
            );
        });
        task.setOnFailed(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            orbitPane.setBusy(false, "Данные орбиты недоступны");
            dialogLayer.showError(
                    "Не удалось загрузить категории и ярлыки",
                    messageFrom(task.getException())
            );
            System.err.println(
                    "[Orbit] Не удалось загрузить accountId="
                            + accountId + ": " + messageFrom(task.getException())
            );
        });
        task.setOnCancelled(event -> {
            if (activeDataTask == task) {
                activeDataTask = null;
            }
        });
        activeDataTask = task;
        DATA_EXECUTOR.execute(task);
    }

    public void cancelProfileLoad() {
        profileLoadGeneration++;
        dataLoadGeneration++;
        cancelProfileTask();
        cancelDataTask();
        cancelLaunchTask();
        dialogLayer.close();
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

    /** @return true when Esc was consumed by a dialog or nested navigation. */
    public boolean handleEscape() {
        if (dialogLayer.handleEscape()) {
            return true;
        }
        return orbitPane.handleEscape();
    }

    public boolean handleHistoryShortcut(KeyEvent event) {
        if (event == null
                || dialogLayer.isDialogOpen()
                || !event.isControlDown()
                || event.getCode() != KeyCode.Z) {
            return false;
        }
        if (event.isShiftDown()) {
            redoLastDeletion();
        } else {
            undoLastDeletion();
        }
        return true;
    }

    public void undoLastDeletion() {
        executeHistoryChange(false);
    }

    public void redoLastDeletion() {
        executeHistoryChange(true);
    }


    public void resetOrbitNavigation() {
        orbitPane.resetNavigation();
    }

    public CircularMenuPane getOrbitPane() {
        return orbitPane;
    }

    private void configureOrbitActions() {
        orbitPane.setCategoryActions(new CircularMenuPane.CategoryActions() {
            @Override
            public void createCategory(Long parentCategoryId, String parentLabel) {
                if (!ensureAuthenticated()) {
                    return;
                }
                boolean root = parentCategoryId == null;
                dialogLayer.showCreate(parentLabel, root, draft -> executeDataMutation(
                        "Создание категории…",
                        "Не удалось создать категорию",
                        () -> {
                            categoryService.createCategory(
                                    session.getAccountId(),
                                    parentCategoryId,
                                    draft
                            );
                            return null;
                        }
                ));
            }

            @Override
            public void editCategory(long categoryId) {
                AppCategory category = categorySnapshot.find(categoryId);
                if (category == null) {
                    showCategoryMissing();
                    return;
                }
                dialogLayer.showEdit(category, draft -> executeDataMutation(
                        "Сохранение категории…",
                        "Не удалось изменить категорию",
                        () -> {
                            categoryService.updateCategory(
                                    session.getAccountId(),
                                    categoryId,
                                    draft
                            );
                            return null;
                        }
                ));
            }

            @Override
            public void moveCategory(long categoryId) {
                AppCategory category = categorySnapshot.find(categoryId);
                if (category == null) {
                    showCategoryMissing();
                    return;
                }
                List<CategoryMoveTarget> targets = categoryService.getMoveTargets(
                        categorySnapshot,
                        categoryId
                );
                dialogLayer.showMove(category, targets, newParentId -> executeDataMutation(
                        "Перемещение категории…",
                        "Не удалось переместить категорию",
                        () -> {
                            categoryService.moveCategory(
                                    session.getAccountId(),
                                    categoryId,
                                    newParentId
                            );
                            return null;
                        }
                ));
            }

            @Override
            public void moveCategoryUp(long categoryId) {
                executeDataMutation(
                        "Изменение общего порядка…",
                        "Не удалось изменить порядок",
                        () -> {
                            categoryService.moveRelative(
                                    session.getAccountId(),
                                    categoryId,
                                    -1
                            );
                            return null;
                        }
                );
            }

            @Override
            public void moveCategoryDown(long categoryId) {
                executeDataMutation(
                        "Изменение общего порядка…",
                        "Не удалось изменить порядок",
                        () -> {
                            categoryService.moveRelative(
                                    session.getAccountId(),
                                    categoryId,
                                    1
                            );
                            return null;
                        }
                );
            }

            @Override
            public void togglePinned(long categoryId) {
                executeDataMutation(
                        "Обновление главной орбиты…",
                        "Не удалось изменить закрепление",
                        () -> {
                            categoryService.togglePinned(
                                    session.getAccountId(),
                                    categoryId
                            );
                            return null;
                        }
                );
            }

            @Override
            public void deleteCategory(long categoryId) {
                AppCategory category = categorySnapshot.find(categoryId);
                if (category == null) {
                    showCategoryMissing();
                    return;
                }
                int categoryCount;
                int shortcutCount;
                try {
                    categoryCount = categoryService.countSubtree(
                            categorySnapshot,
                            categoryId
                    );
                    shortcutCount = categoryService.countShortcutsInSubtree(
                            categorySnapshot,
                            categoryId
                    );
                } catch (RuntimeException exception) {
                    dialogLayer.showError("Ошибка дерева", exception.getMessage());
                    return;
                }
                dialogLayer.showDelete(
                        category,
                        categoryCount,
                        shortcutCount,
                        () -> executeUndoableDeletion(
                                "Удаление поддерева…",
                                "Не удалось удалить категорию",
                                new DeleteCategoryCommand(
                                        categoryService,
                                        session.getAccountId(),
                                        categoryId,
                                        category.getName()
                                ),
                                "Категория удалена — Ctrl+Z для отмены"
                        )
                );
            }

            @Override
            public void refreshCategories() {
                loadCategoriesAsync(true);
            }

            @Override
            public void createShortcut(long categoryId, String categoryLabel) {
                if (!ensureAuthenticated()) {
                    return;
                }
                if (categorySnapshot.find(categoryId) == null) {
                    showCategoryMissing();
                    return;
                }
                dialogLayer.showCreateShortcut(
                        categoryLabel,
                        draft -> executeDataMutation(
                                "Создание ярлыка…",
                                "Не удалось создать ярлык",
                                () -> {
                                    shortcutService.createShortcut(
                                            session.getAccountId(),
                                            categoryId,
                                            draft
                                    );
                                    return null;
                                }
                        )
                );
            }

            @Override
            public void editShortcut(long shortcutId) {
                AppShortcut shortcut = categorySnapshot.findShortcut(shortcutId);
                if (shortcut == null) {
                    showShortcutMissing();
                    return;
                }
                dialogLayer.showEditShortcut(
                        shortcut,
                        draft -> executeDataMutation(
                                "Сохранение ярлыка…",
                                "Не удалось изменить ярлык",
                                () -> {
                                    shortcutService.updateShortcut(
                                            session.getAccountId(),
                                            shortcutId,
                                            draft
                                    );
                                    return null;
                                }
                        )
                );
            }

            @Override
            public void moveShortcut(long shortcutId) {
                AppShortcut shortcut = categorySnapshot.findShortcut(shortcutId);
                if (shortcut == null) {
                    showShortcutMissing();
                    return;
                }
                List<ShortcutMoveTarget> targets = shortcutService.getMoveTargets(
                        categorySnapshot,
                        shortcutId
                );
                dialogLayer.showMoveShortcut(
                        shortcut,
                        targets,
                        categoryId -> executeDataMutation(
                                "Перемещение ярлыка…",
                                "Не удалось переместить ярлык",
                                () -> {
                                    shortcutService.moveShortcut(
                                            session.getAccountId(),
                                            shortcutId,
                                            categoryId
                                    );
                                    return null;
                                }
                        )
                );
            }

            @Override
            public void moveShortcutUp(long shortcutId) {
                executeDataMutation(
                        "Изменение общего порядка…",
                        "Не удалось изменить порядок",
                        () -> {
                            shortcutService.moveRelative(
                                    session.getAccountId(),
                                    shortcutId,
                                    -1
                            );
                            return null;
                        }
                );
            }

            @Override
            public void moveShortcutDown(long shortcutId) {
                executeDataMutation(
                        "Изменение общего порядка…",
                        "Не удалось изменить порядок",
                        () -> {
                            shortcutService.moveRelative(
                                    session.getAccountId(),
                                    shortcutId,
                                    1
                            );
                            return null;
                        }
                );
            }

            @Override
            public void deleteShortcut(long shortcutId) {
                AppShortcut shortcut = categorySnapshot.findShortcut(shortcutId);
                if (shortcut == null) {
                    showShortcutMissing();
                    return;
                }
                dialogLayer.showDeleteShortcut(
                        shortcut,
                        () -> executeUndoableDeletion(
                                "Удаление ярлыка…",
                                "Не удалось удалить ярлык",
                                new DeleteShortcutCommand(
                                        shortcutService,
                                        session.getAccountId(),
                                        shortcutId,
                                        shortcut.getDisplayName()
                                ),
                                "Ярлык удалён — Ctrl+Z для отмены"
                        )
                );
            }


            @Override
            public void undoLastDeletion() {
                ProfileView.this.undoLastDeletion();
            }

            @Override
            public void redoLastDeletion() {
                ProfileView.this.redoLastDeletion();
            }

            @Override
            public void launchShortcut(long shortcutId) {
                launchShortcutAsync(shortcutId);
            }
        });
    }


    private void launchShortcutAsync(long shortcutId) {
        AppShortcut shortcut = categorySnapshot.findShortcut(shortcutId);
        if (shortcut == null) {
            showShortcutMissing();
            return;
        }
        if (activeLaunchTask != null) {
            orbitPane.setStatus("Дождитесь завершения предыдущего запуска.");
            return;
        }

        long generation = ++launchGeneration;
        orbitPane.setBusy(true, "Запуск «" + shortcut.getDisplayName() + "»…");
        Task<LaunchResult> task = new Task<>() {
            @Override
            protected LaunchResult call() {
                return appLauncher.launch(shortcut);
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != launchGeneration) {
                return;
            }
            activeLaunchTask = null;
            LaunchResult result = task.getValue();
            if (result != null && result.success()) {
                orbitPane.setBusy(false, result.message());
                System.out.println("[Launcher] " + result.message());
                onShortcutLaunched.run();
            } else {
                String message = result == null
                        ? "Стратегия запуска не вернула результат."
                        : result.message();
                orbitPane.setBusy(false, "Запуск не выполнен");
                dialogLayer.showError("Не удалось запустить ярлык", message);
                System.err.println("[Launcher] " + message);
            }
        });
        task.setOnFailed(event -> {
            if (generation != launchGeneration) {
                return;
            }
            activeLaunchTask = null;
            String message = messageFrom(task.getException());
            orbitPane.setBusy(false, "Запуск не выполнен");
            dialogLayer.showError("Не удалось запустить ярлык", message);
            System.err.println("[Launcher] " + message);
        });
        task.setOnCancelled(event -> {
            if (activeLaunchTask == task) {
                activeLaunchTask = null;
                orbitPane.setBusy(false, "Запуск отменён");
            }
        });
        activeLaunchTask = task;
        DATA_EXECUTOR.execute(task);
    }

    private void executeUndoableDeletion(
            String busyMessage,
            String errorTitle,
            UndoableCommand command,
            String successMessage
    ) {
        if (!ensureAuthenticated()) {
            return;
        }
        cancelDataTask();
        long generation = ++dataLoadGeneration;
        long accountId = session.getAccountId();
        List<Long> preferredPath = orbitPane.getNavigationPathCategoryIds();
        boolean showAllRoots = orbitPane.isShowingAllRootCategories();

        UndoManager history = undoManager;
        dialogLayer.showBusy(busyMessage);
        orbitPane.setBusy(true, busyMessage);
        Task<CategorySnapshot> task = new Task<>() {
            @Override
            protected CategorySnapshot call() throws Exception {
                history.execute(command);
                return categoryService.loadSnapshot(accountId);
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            dialogLayer.close();
            applyCategorySnapshot(task.getValue(), preferredPath, showAllRoots);
            updateUndoState();
            orbitPane.setBusy(false, successMessage);
            System.out.println("[Undo] Добавлено в историю: " + command.description());
        });
        task.setOnFailed(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            orbitPane.setBusy(false, "Удаление не выполнено");
            dialogLayer.showError(errorTitle, messageFrom(task.getException()));
            System.err.println("[Undo] Ошибка удаления: " + messageFrom(task.getException()));
        });
        task.setOnCancelled(event -> {
            if (activeDataTask == task) {
                activeDataTask = null;
            }
        });
        activeDataTask = task;
        DATA_EXECUTOR.execute(task);
    }

    private void executeHistoryChange(boolean redo) {
        if (!ensureAuthenticated()) {
            return;
        }
        if (dialogLayer.isDialogOpen()) {
            return;
        }
        if (activeDataTask != null) {
            orbitPane.setStatus("Дождитесь завершения текущей операции.");
            return;
        }

        UndoManager history = undoManager;
        String description = (redo
                ? history.nextRedoDescription()
                : history.nextUndoDescription())
                .orElse(null);
        if (description == null) {
            orbitPane.setStatus(redo
                    ? "Нет действий для повтора."
                    : "Нет действий для отмены.");
            updateUndoState();
            return;
        }

        long generation = ++dataLoadGeneration;
        long accountId = session.getAccountId();
        List<Long> preferredPath = orbitPane.getNavigationPathCategoryIds();
        boolean showAllRoots = orbitPane.isShowingAllRootCategories();
        String busyMessage = redo
                ? "Повтор действия…"
                : "Отмена действия…";
        String errorTitle = redo
                ? "Не удалось повторить действие"
                : "Не удалось отменить действие";

        dialogLayer.showBusy(busyMessage);
        orbitPane.setBusy(true, busyMessage);
        Task<CategorySnapshot> task = new Task<>() {
            @Override
            protected CategorySnapshot call() throws Exception {
                if (redo) {
                    history.redo();
                } else {
                    history.undo();
                }
                return categoryService.loadSnapshot(accountId);
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            dialogLayer.close();
            applyCategorySnapshot(task.getValue(), preferredPath, showAllRoots);
            updateUndoState();
            orbitPane.setBusy(
                    false,
                    (redo ? "Повторено: " : "Отменено: ") + description
            );
            System.out.println(
                    "[Undo] " + (redo ? "Повторено: " : "Отменено: ") + description
            );
        });
        task.setOnFailed(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            orbitPane.setBusy(false, "История не изменена");
            updateUndoState();
            dialogLayer.showError(errorTitle, messageFrom(task.getException()));
            System.err.println("[Undo] Ошибка: " + messageFrom(task.getException()));
        });
        task.setOnCancelled(event -> {
            if (activeDataTask == task) {
                activeDataTask = null;
            }
        });
        activeDataTask = task;
        DATA_EXECUTOR.execute(task);
    }

    private void updateUndoState() {
        orbitPane.setUndoState(
                undoManager.canUndo(),
                undoManager.nextUndoDescription().orElse(""),
                undoManager.canRedo(),
                undoManager.nextRedoDescription().orElse("")
        );
    }

    private void executeDataMutation(
            String busyMessage,
            String errorTitle,
            Callable<Void> operation
    ) {
        if (!ensureAuthenticated()) {
            return;
        }
        cancelDataTask();
        long generation = ++dataLoadGeneration;
        long accountId = session.getAccountId();
        List<Long> preferredPath = orbitPane.getNavigationPathCategoryIds();
        boolean showAllRoots = orbitPane.isShowingAllRootCategories();

        dialogLayer.showBusy(busyMessage);
        orbitPane.setBusy(true, busyMessage);
        Task<CategorySnapshot> task = new Task<>() {
            @Override
            protected CategorySnapshot call() throws Exception {
                operation.call();
                return categoryService.loadSnapshot(accountId);
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            dialogLayer.close();
            applyCategorySnapshot(task.getValue(), preferredPath, showAllRoots);
            // Operations that are not command-based can invalidate the exact
            // ordering captured by deletion commands. Until those operations
            // become undoable too, start a fresh safe history boundary.
            undoManager.clear();
            updateUndoState();
            orbitPane.setBusy(false, "Изменения сохранены");
        });
        task.setOnFailed(event -> {
            if (generation != dataLoadGeneration || accountId != session.getAccountId()) {
                return;
            }
            activeDataTask = null;
            orbitPane.setBusy(false, "Операция не выполнена");
            dialogLayer.showError(errorTitle, messageFrom(task.getException()));
            System.err.println("[Orbit] Ошибка операции: " + messageFrom(task.getException()));
        });
        task.setOnCancelled(event -> {
            if (activeDataTask == task) {
                activeDataTask = null;
            }
        });
        activeDataTask = task;
        DATA_EXECUTOR.execute(task);
    }

    private void applyCategorySnapshot(
            CategorySnapshot snapshot,
            List<Long> preferredPath,
            boolean showAllRoots
    ) {
        categorySnapshot = snapshot == null
                ? emptySnapshot(session.getAccountId())
                : snapshot;
        orbitPane.setCategoryData(
                categorySnapshot.getPinnedRootEntries(),
                categorySnapshot.getAllRootEntries(),
                preferredPath,
                showAllRoots
        );
        updateUndoState();
    }

    private boolean ensureAuthenticated() {
        if (session.isAuthenticated()) {
            return true;
        }
        dialogLayer.showError(
                "Требуется вход",
                "Управление категориями и ярлыками доступно только авторизованному пользователю."
        );
        return false;
    }

    private void showCategoryMissing() {
        dialogLayer.showError(
                "Категория не найдена",
                "Данные могли измениться. Нажмите ↻, чтобы обновить орбиту."
        );
    }

    private void showShortcutMissing() {
        dialogLayer.showError(
                "Ярлык не найден",
                "Данные могли измениться. Нажмите ↻, чтобы обновить орбиту."
        );
    }

    private void cancelProfileTask() {
        if (activeProfileTask != null) {
            activeProfileTask.cancel(true);
            activeProfileTask = null;
        }
    }

    private void cancelDataTask() {
        if (activeDataTask != null) {
            activeDataTask.cancel(true);
            activeDataTask = null;
        }
    }


    private void cancelLaunchTask() {
        launchGeneration++;
        if (activeLaunchTask != null) {
            activeLaunchTask.cancel(true);
            activeLaunchTask = null;
        }
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
        dialogLayer.resizeRelocate(0, 0, width, height);
    }

    private void loadStylesheet(String resourcePath) {
        URL stylesheet = ProfileView.class.getResource(resourcePath);
        if (stylesheet != null) {
            getStylesheets().add(stylesheet.toExternalForm());
        } else {
            System.err.println("[Profile] Не найден stylesheet " + resourcePath);
        }
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

    private String messageFrom(Throwable throwable) {
        if (throwable == null) {
            return "Неизвестная ошибка";
        }
        Throwable current = throwable;
        while (current.getCause() != null
                && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private CategorySnapshot emptySnapshot(long accountId) {
        return new CategorySnapshot(
                accountId,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private int counter;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                    runnable,
                    "saoim-data-loader-" + (++counter)
            );
            thread.setDaemon(true);
            return thread;
        }
    }
}
