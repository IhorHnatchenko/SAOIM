package org.example;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * Data-driven circular menu with eight entries per page, nested categories and
 * mixed category/shortcut management.
 */
public final class CircularMenuPane extends Pane {
    public static final int PAGE_SIZE = 8;
    private static final int TICK_COUNT = 32;

    /**
     * Existing category methods remain abstract for compatibility with step 6.
     * Shortcut methods are defaults so an older implementation still compiles.
     */
    public interface CategoryActions {
        void createCategory(Long parentCategoryId, String parentLabel);

        void editCategory(long categoryId);

        void moveCategory(long categoryId);

        void moveCategoryUp(long categoryId);

        void moveCategoryDown(long categoryId);

        void togglePinned(long categoryId);

        void deleteCategory(long categoryId);

        void refreshCategories();

        default void createShortcut(long categoryId, String categoryLabel) {
        }

        default void editShortcut(long shortcutId) {
        }

        default void moveShortcut(long shortcutId) {
        }

        default void moveShortcutUp(long shortcutId) {
        }

        default void moveShortcutDown(long shortcutId) {
        }

        default void deleteShortcut(long shortcutId) {
        }

        default void launchShortcut(long shortcutId) {
        }

        default void undoLastDeletion() {
        }

        default void redoLastDeletion() {
        }
    }

    private final Circle orbitBackdrop = createCircle("orbit-backdrop");
    private final Circle outerGlowRing = createRing("orbit-ring--outer-glow");
    private final Circle outerRing = createRing("orbit-ring--outer");
    private final Circle middleRing = createRing("orbit-ring--middle");
    private final Circle innerRing = createRing("orbit-ring--inner");
    private final Circle coreRing = createRing("orbit-ring--core");
    private final Line[] tickLines = new Line[TICK_COUNT];
    private final Line[] connectorGlowLines = new Line[PAGE_SIZE];
    private final Line[] connectorLines = new Line[PAGE_SIZE];
    private final Circle[] anchorNodes = new Circle[PAGE_SIZE];

    private final CommandCrystal commandCrystal = new CommandCrystal();
    private final Button previousPageButton = createPageButton("‹", "Предыдущая страница");
    private final Button nextPageButton = createPageButton("›", "Следующая страница");
    private final Label pageIndicator = new Label();
    private final Label breadcrumbLabel = new Label();
    private final Label statusLabel = new Label();

    private final Button showAllButton = createToolButton(
            "Все",
            "Показать все или только закреплённые корневые категории"
    );
    private final Button createCategoryButton = createToolButton(
            "＋",
            "Создать категорию (Insert)"
    );
    private final Button createShortcutButton = createToolButton(
            "＋↗",
            "Создать ярлык в текущей категории (Ctrl+Insert)"
    );
    private final Button launchButton = createToolButton(
            "▶",
            "Запустить выбранный ярлык (Enter)"
    );
    private final Button editButton = createToolButton(
            "✎",
            "Изменить выбранный элемент (F2)"
    );
    private final Button moveButton = createToolButton(
            "⇄",
            "Переместить выбранный элемент"
    );
    private final Button upButton = createToolButton(
            "↑",
            "Поднять выше в общем порядке (Ctrl+Up)"
    );
    private final Button downButton = createToolButton(
            "↓",
            "Опустить ниже в общем порядке (Ctrl+Down)"
    );
    private final Button pinButton = createToolButton(
            "★",
            "Закрепить или открепить корневую категорию (Ctrl+P)"
    );
    private final Button deleteButton = createToolButton(
            "⌫",
            "Удалить выбранный элемент (Delete)"
    );
    private final Button undoButton = createToolButton(
            "↶",
            "Отменить последнее удаление (Ctrl+Z)"
    );
    private final Button redoButton = createToolButton(
            "↷",
            "Повторить отменённое удаление (Ctrl+Shift+Z)"
    );
    private final Button refreshButton = createToolButton(
            "↻",
            "Перечитать категории и ярлыки из базы"
    );

    private final HBox toolBar = new HBox(
            7,
            showAllButton,
            createCategoryButton,
            createShortcutButton,
            launchButton,
            editButton,
            moveButton,
            upButton,
            downButton,
            pinButton,
            deleteButton,
            undoButton,
            redoButton,
            refreshButton
    );

    private final OrbitNavigationStack navigation;
    private final List<OrbitItemView> visibleItems = new ArrayList<>();

    private List<OrbitEntry> pinnedRootEntries = List.of();
    private List<OrbitEntry> allRootEntries = List.of();
    private CategoryActions categoryActions;
    private final ShortcutAvailabilityService availabilityService =
            new ShortcutAvailabilityService();
    private final IconService iconService = new IconService();
    private Function<Long, AppShortcut> shortcutResolver = ignored -> null;
    private int pageIndex;
    private OrbitItemView selectedItem;
    private boolean showAllRootCategories;
    private boolean busy;
    private boolean canUndoDeletion;
    private boolean canRedoDeletion;
    private String undoDescription = "";
    private String redoDescription = "";

    public CircularMenuPane(List<OrbitEntry> rootEntries) {
        getStyleClass().add("circular-menu-pane");
        setPickOnBounds(false);
        setFocusTraversable(true);

        pinnedRootEntries = safeCopy(rootEntries);
        allRootEntries = safeCopy(rootEntries);
        navigation = new OrbitNavigationStack(pinnedRootEntries);

        pageIndicator.getStyleClass().add("orbit-page-indicator");
        breadcrumbLabel.getStyleClass().add("orbit-breadcrumb");
        breadcrumbLabel.setAlignment(Pos.CENTER);
        breadcrumbLabel.setMouseTransparent(true);
        statusLabel.getStyleClass().add("orbit-status");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMouseTransparent(true);
        toolBar.getStyleClass().add("orbit-toolbar");
        toolBar.setAlignment(Pos.CENTER);

        getChildren().addAll(
                orbitBackdrop,
                outerGlowRing,
                outerRing,
                middleRing,
                innerRing,
                coreRing
        );
        createTicks();
        createConnectors();
        getChildren().addAll(
                commandCrystal,
                previousPageButton,
                nextPageButton,
                pageIndicator,
                breadcrumbLabel,
                statusLabel,
                toolBar
        );

        previousPageButton.setOnAction(event -> showPreviousPage());
        nextPageButton.setOnAction(event -> showNextPage());

        commandCrystal.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (navigation.canGoBack()) {
                    navigateBack();
                } else {
                    clearSelection();
                    updateCrystalForCurrentLevel();
                }
                event.consume();
            }
        });
        commandCrystal.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                if (navigation.canGoBack()) {
                    navigateBack();
                }
                event.consume();
            }
        });
        consumeSecondaryClicks(commandCrystal);

        showAllButton.setOnAction(event -> toggleRootMode());
        createCategoryButton.setOnAction(event -> createCategory());
        createShortcutButton.setOnAction(event -> createShortcut());
        launchButton.setOnAction(event -> launchSelected());
        editButton.setOnAction(event -> editSelected());
        moveButton.setOnAction(event -> moveSelected());
        upButton.setOnAction(event -> moveSelectedRelative(-1));
        downButton.setOnAction(event -> moveSelectedRelative(1));
        pinButton.setOnAction(event -> withSelectedCategory(
                categoryActions == null ? null : categoryActions::togglePinned
        ));
        deleteButton.setOnAction(event -> deleteSelected());
        undoButton.setOnAction(event -> {
            if (categoryActions != null && !busy && canUndoDeletion) {
                categoryActions.undoLastDeletion();
            }
        });
        redoButton.setOnAction(event -> {
            if (categoryActions != null && !busy && canRedoDeletion) {
                categoryActions.redoLastDeletion();
            }
        });
        refreshButton.setOnAction(event -> {
            if (categoryActions != null && !busy) {
                categoryActions.refreshCategories();
            }
        });

        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyboardShortcut);
        refreshCurrentLevel();
    }

    public void setCategoryActions(CategoryActions categoryActions) {
        this.categoryActions = categoryActions;
        updateToolBarState();
    }

    public void setShortcutResolver(Function<Long, AppShortcut> shortcutResolver) {
        this.shortcutResolver = shortcutResolver == null ? ignored -> null : shortcutResolver;
        refreshCurrentLevel();
    }

    public void setUndoState(
            boolean canUndo,
            String undoDescription,
            boolean canRedo,
            String redoDescription
    ) {
        this.canUndoDeletion = canUndo;
        this.canRedoDeletion = canRedo;
        this.undoDescription = normalizeHistoryDescription(undoDescription);
        this.redoDescription = normalizeHistoryDescription(redoDescription);
        updateToolBarState();
    }


    public void setCategoryData(
            List<OrbitEntry> pinnedRoots,
            List<OrbitEntry> allRoots,
            List<Long> preferredPathIds,
            boolean showAllRoots
    ) {
        pinnedRootEntries = safeCopy(pinnedRoots);
        allRootEntries = safeCopy(allRoots);
        showAllRootCategories = showAllRoots || pinnedRootEntries.isEmpty();
        List<OrbitEntry> activeRoots = getActiveRootEntries();
        navigation.replaceRootEntries(activeRoots, preferredPathIds);
        pageIndex = 0;
        clearSelection();
        setStatus(activeRoots.isEmpty() ? "Категорий пока нет — нажмите ＋" : "");
        refreshCurrentLevel();
    }

    public List<Long> getNavigationPathCategoryIds() {
        return navigation.getPathCategoryIds();
    }

    public boolean isShowingAllRootCategories() {
        return showAllRootCategories;
    }

    public OrbitEntry getSelectedEntry() {
        return selectedItem == null ? null : selectedItem.getEntry();
    }

    public Long getCurrentParentCategoryId() {
        return navigation.getCurrentCategoryId();
    }

    public String getCurrentParentLabel() {
        return navigation.isAtRoot()
                ? "Корень категорий"
                : navigation.getBreadcrumb();
    }

    public void setBusy(boolean busy, String message) {
        this.busy = busy;
        if (message != null) {
            setStatus(message);
        }
        updateToolBarState();
    }

    public void setStatus(String message) {
        String safe = message == null ? "" : message;
        statusLabel.setText(safe);
        statusLabel.setVisible(!safe.isBlank());
        statusLabel.setManaged(statusLabel.isVisible());
        requestLayout();
    }

    public boolean handleEscape() {
        if (!navigation.canGoBack()) {
            return false;
        }
        navigateBack();
        return true;
    }

    public void resetNavigation() {
        showAllRootCategories = pinnedRootEntries.isEmpty();
        navigation.replaceRootEntries(getActiveRootEntries(), List.of());
        pageIndex = 0;
        clearSelection();
        refreshCurrentLevel();
    }

    public boolean isAtRoot() {
        return navigation.isAtRoot();
    }

    public OrbitNavigationStack getNavigation() {
        return navigation;
    }

    private void handleKeyboardShortcut(KeyEvent event) {
        if (busy) {
            return;
        }
        if (event.getCode() == KeyCode.PAGE_UP && !previousPageButton.isDisabled()) {
            showPreviousPage();
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_DOWN && !nextPageButton.isDisabled()) {
            showNextPage();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.INSERT) {
            createShortcut();
            event.consume();
        } else if (event.getCode() == KeyCode.INSERT) {
            createCategory();
            event.consume();
        } else if (event.getCode() == KeyCode.F2) {
            editSelected();
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE) {
            deleteSelected();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.UP) {
            moveSelectedRelative(-1);
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.DOWN) {
            moveSelectedRelative(1);
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.P) {
            withSelectedCategory(categoryActions == null
                    ? null
                    : categoryActions::togglePinned);
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
            open(selectedItem);
            event.consume();
        }
    }

    private void createTicks() {
        for (int i = 0; i < tickLines.length; i++) {
            Line tick = new Line();
            tick.getStyleClass().add("orbit-tick");
            if (i % 4 == 0) {
                tick.getStyleClass().add("orbit-tick--major");
            }
            tick.setMouseTransparent(true);
            tickLines[i] = tick;
            getChildren().add(tick);
        }
    }

    private void createConnectors() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            Line glowLine = new Line();
            glowLine.getStyleClass().add("orbit-connector-glow");
            glowLine.setMouseTransparent(true);
            connectorGlowLines[i] = glowLine;
            getChildren().add(glowLine);

            Line line = new Line();
            line.getStyleClass().add("orbit-connector");
            line.setMouseTransparent(true);
            connectorLines[i] = line;
            getChildren().add(line);

            Circle anchor = createCircle("orbit-anchor");
            anchor.setMouseTransparent(true);
            anchorNodes[i] = anchor;
            getChildren().add(anchor);
        }
    }

    private void select(OrbitItemView item) {
        clearSelection();
        selectedItem = item;
        selectedItem.setSelected(true);
        OrbitEntry entry = item.getEntry();

        String subtitle;
        if (entry.isRootPinned()) {
            subtitle = "Закреплена на главной орбите";
        } else if (entry.isShortcut()) {
            subtitle = (entry.isEnabled() ? "Ярлык · " : "Отключён · ")
                    + (entry.getLaunchType() == null
                    ? "неизвестный тип"
                    : entry.getLaunchType().getDisplayName());
        } else {
            subtitle = entry.getDescription();
        }
        commandCrystal.setSelectionText(entry.getTitle(), subtitle);
        commandCrystal.setBackAvailable(navigation.canGoBack());
        updateToolBarState();
    }

    private void open(OrbitItemView item) {
        OrbitEntry entry = item.getEntry();
        if (entry.isCategory()) {
            navigation.enter(entry);
            pageIndex = 0;
            clearSelection();
            setStatus("");
            refreshCurrentLevel();
            requestFocus();
            System.out.println("[Orbit] Открыта категория: " + entry.getTitle());
            return;
        }
        select(item);
        if (entry.isShortcut()) {
            launchShortcut(entry, item.getAvailability());
        }
    }

    private void clearSelection() {
        if (selectedItem != null) {
            selectedItem.setSelected(false);
            selectedItem = null;
        }
        updateToolBarState();
    }

    private void navigateBack() {
        if (!navigation.goBack()) {
            return;
        }
        pageIndex = 0;
        clearSelection();
        setStatus("");
        refreshCurrentLevel();
        requestFocus();
        System.out.println("[Orbit] Возврат: " + navigation.getBreadcrumb());
    }

    private void toggleRootMode() {
        if (!navigation.isAtRoot() || busy) {
            return;
        }
        showAllRootCategories = !showAllRootCategories;
        if (!showAllRootCategories && pinnedRootEntries.isEmpty()) {
            showAllRootCategories = true;
            setStatus("Нет закреплённых категорий.");
        }
        navigation.replaceRootEntries(getActiveRootEntries(), List.of());
        pageIndex = 0;
        clearSelection();
        refreshCurrentLevel();
    }

    private void createCategory() {
        if (categoryActions == null || busy) {
            return;
        }
        categoryActions.createCategory(
                getCurrentParentCategoryId(),
                getCurrentParentLabel()
        );
    }

    private void createShortcut() {
        if (categoryActions == null || busy) {
            return;
        }
        Long parentId = getCurrentParentCategoryId();
        if (parentId == null) {
            setStatus("Сначала откройте категорию — ярлыки нельзя хранить в корне");
            return;
        }
        categoryActions.createShortcut(parentId, getCurrentParentLabel());
    }


    private void launchSelected() {
        OrbitEntry entry = selectedEntryOrNull();
        if (entry == null || !entry.isDatabaseShortcut()) {
            return;
        }
        LaunchAvailability availability = selectedItem == null
                ? LaunchAvailability.available("")
                : selectedItem.getAvailability();
        launchShortcut(entry, availability);
    }

    private void launchShortcut(OrbitEntry entry, LaunchAvailability availability) {
        if (categoryActions == null || busy || !entry.isDatabaseShortcut()) {
            return;
        }
        if (!entry.isEnabled()) {
            setStatus("Ярлык отключён. Откройте редактор F2, чтобы включить его.");
            return;
        }
        if (availability != null && availability.checking()) {
            setStatus("Дождитесь завершения проверки ярлыка.");
            return;
        }
        if (availability != null && !availability.available()) {
            setStatus(availability.message());
            return;
        }
        setStatus("Запуск: " + entry.getTitle() + "…");
        categoryActions.launchShortcut(entry.getShortcutId());
    }

    private void editSelected() {
        OrbitEntry entry = selectedEntryOrNull();
        if (entry == null || categoryActions == null || busy) {
            return;
        }
        if (entry.isDatabaseCategory()) {
            categoryActions.editCategory(entry.getCategoryId());
        } else if (entry.isDatabaseShortcut()) {
            categoryActions.editShortcut(entry.getShortcutId());
        }
    }

    private void moveSelected() {
        OrbitEntry entry = selectedEntryOrNull();
        if (entry == null || categoryActions == null || busy) {
            return;
        }
        if (entry.isDatabaseCategory()) {
            categoryActions.moveCategory(entry.getCategoryId());
        } else if (entry.isDatabaseShortcut()) {
            categoryActions.moveShortcut(entry.getShortcutId());
        }
    }

    private void moveSelectedRelative(int direction) {
        OrbitEntry entry = selectedEntryOrNull();
        if (entry == null || categoryActions == null || busy) {
            return;
        }
        if (entry.isDatabaseCategory()) {
            if (direction < 0) {
                categoryActions.moveCategoryUp(entry.getCategoryId());
            } else {
                categoryActions.moveCategoryDown(entry.getCategoryId());
            }
        } else if (entry.isDatabaseShortcut()) {
            if (direction < 0) {
                categoryActions.moveShortcutUp(entry.getShortcutId());
            } else {
                categoryActions.moveShortcutDown(entry.getShortcutId());
            }
        }
    }

    private void deleteSelected() {
        OrbitEntry entry = selectedEntryOrNull();
        if (entry == null || categoryActions == null || busy) {
            return;
        }
        if (entry.isDatabaseCategory()) {
            categoryActions.deleteCategory(entry.getCategoryId());
        } else if (entry.isDatabaseShortcut()) {
            categoryActions.deleteShortcut(entry.getShortcutId());
        }
    }

    private OrbitEntry selectedEntryOrNull() {
        return selectedItem == null ? null : selectedItem.getEntry();
    }

    private void withSelectedCategory(LongConsumer action) {
        OrbitEntry entry = selectedEntryOrNull();
        if (action == null || busy || entry == null || !entry.isDatabaseCategory()) {
            return;
        }
        action.accept(entry.getCategoryId());
    }

    private void showPreviousPage() {
        if (busy || pageIndex <= 0) {
            return;
        }
        pageIndex--;
        clearSelection();
        refreshCurrentLevel();
    }

    private void showNextPage() {
        if (busy || pageIndex + 1 >= totalPages()) {
            return;
        }
        pageIndex++;
        clearSelection();
        refreshCurrentLevel();
    }

    private void refreshCurrentLevel() {
        removeVisibleItems();
        List<OrbitEntry> entries = navigation.getCurrentEntries();
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));
        int from = Math.min(entries.size(), pageIndex * PAGE_SIZE);
        int to = Math.min(entries.size(), from + PAGE_SIZE);

        for (OrbitEntry entry : entries.subList(from, to)) {
            AppShortcut shortcut = entry.isDatabaseShortcut()
                    ? shortcutResolver.apply(entry.getShortcutId())
                    : null;
            OrbitItemView item = new OrbitItemView(
                    entry,
                    shortcut,
                    availabilityService,
                    iconService,
                    this::updateToolBarState
            );
            item.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    select(item);
                    if (event.getClickCount() >= 2) {
                        open(item);
                    }
                    event.consume();
                }
            });
            item.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    open(item);
                    event.consume();
                } else if (event.getCode() == KeyCode.SPACE) {
                    select(item);
                    event.consume();
                }
            });
            visibleItems.add(item);
            getChildren().add(item);
        }

        previousPageButton.setDisable(pageIndex <= 0);
        nextPageButton.setDisable(pageIndex + 1 >= totalPages);
        previousPageButton.setVisible(totalPages > 1);
        nextPageButton.setVisible(totalPages > 1);
        previousPageButton.setManaged(previousPageButton.isVisible());
        nextPageButton.setManaged(nextPageButton.isVisible());
        pageIndicator.setText((pageIndex + 1) + " / " + totalPages);
        pageIndicator.setVisible(totalPages > 1);
        pageIndicator.setManaged(pageIndicator.isVisible());

        breadcrumbLabel.setText(navigation.getBreadcrumb());
        updateCrystalForCurrentLevel();
        updateToolBarState();
        requestLayout();
    }

    private void updateCrystalForCurrentLevel() {
        commandCrystal.setLevelText(
                navigation.getCurrentTitle(),
                navigation.getBreadcrumb(),
                navigation.canGoBack()
        );
    }

    private void updateToolBarState() {
        OrbitEntry selected = selectedEntryOrNull();
        boolean hasSelection = selected != null
                && (selected.isDatabaseCategory() || selected.isDatabaseShortcut());
        boolean selectedCategory = selected != null && selected.isDatabaseCategory();
        boolean selectedShortcut = selected != null && selected.isDatabaseShortcut();
        boolean selectedShortcutAvailable = selectedShortcut
                && selectedItem != null
                && selectedItem.getAvailability().available();
        boolean atRoot = navigation.isAtRoot();
        boolean insideCategory = navigation.getCurrentCategoryId() != null;

        showAllButton.setDisable(busy || !atRoot);
        showAllButton.setText(showAllRootCategories ? "★" : "Все");
        Tooltip.install(
                showAllButton,
                new Tooltip(showAllRootCategories
                        ? "Показать только закреплённые категории"
                        : "Показать все корневые категории")
        );

        createCategoryButton.setDisable(busy || categoryActions == null);
        createShortcutButton.setDisable(
                busy || categoryActions == null || !insideCategory
        );
        launchButton.setDisable(
                busy || categoryActions == null || !selectedShortcutAvailable
        );
        editButton.setDisable(busy || !hasSelection);
        moveButton.setDisable(busy || !hasSelection);
        upButton.setDisable(busy || !hasSelection);
        downButton.setDisable(busy || !hasSelection);
        pinButton.setDisable(busy || !selectedCategory || !atRoot);
        deleteButton.setDisable(busy || !hasSelection);
        undoButton.setDisable(busy || categoryActions == null || !canUndoDeletion);
        redoButton.setDisable(busy || categoryActions == null || !canRedoDeletion);
        Tooltip.install(
                undoButton,
                new Tooltip(canUndoDeletion
                        ? "Отменить: " + undoDescription + " (Ctrl+Z)"
                        : "Нет удалений для отмены")
        );
        Tooltip.install(
                redoButton,
                new Tooltip(canRedoDeletion
                        ? "Повторить: " + redoDescription + " (Ctrl+Shift+Z)"
                        : "Нет действий для повтора")
        );
        refreshButton.setDisable(busy || categoryActions == null);
    }

    private String normalizeHistoryDescription(String description) {
        return description == null || description.isBlank()
                ? "последнее удаление"
                : description.trim();
    }

    private int totalPages() {
        int count = navigation.getCurrentEntries().size();
        return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private List<OrbitEntry> getActiveRootEntries() {
        return showAllRootCategories ? allRootEntries : pinnedRootEntries;
    }

    private void removeVisibleItems() {
        getChildren().removeAll(visibleItems);
        visibleItems.clear();
        for (int i = 0; i < PAGE_SIZE; i++) {
            connectorGlowLines[i].setVisible(false);
            connectorLines[i].setVisible(false);
            anchorNodes[i].setVisible(false);
        }
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double centerX = width / 2.0;
        double toolbarHeight = Math.max(46, toolBar.prefHeight(-1));
        double reservedBottom = toolbarHeight + 78;
        double usableHeight = Math.max(320, height - reservedBottom);
        double centerY = Math.max(usableHeight * 0.50, 190);
        double base = Math.min(width, usableHeight);
        double radius = clamp(base * 0.36, 190, 390);
        double itemScale = clamp(base / 850.0, 0.82, 1.08);
        double itemWidth = 112 * itemScale;
        double itemHeight = 96 * itemScale;
        double crystalSize = clamp(radius * 0.83, 170, 310);

        orbitBackdrop.setCenterX(centerX);
        orbitBackdrop.setCenterY(centerY);
        orbitBackdrop.setRadius(radius * 1.13);
        setRing(outerGlowRing, centerX, centerY, radius * 1.04);
        setRing(outerRing, centerX, centerY, radius);
        setRing(middleRing, centerX, centerY, radius * 0.82);
        setRing(innerRing, centerX, centerY, radius * 0.63);
        setRing(coreRing, centerX, centerY, radius * 0.44);

        layoutTicks(centerX, centerY, radius);

        commandCrystal.resizeRelocate(
                centerX - crystalSize / 2.0,
                centerY - crystalSize / 2.0,
                crystalSize,
                crystalSize
        );

        int itemCount = visibleItems.size();
        double angleStep = itemCount == 0 ? 0 : 360.0 / itemCount;
        double startAngle = -90.0;
        double connectorStartRadius = crystalSize * 0.40;
        double itemRadius = radius;

        for (int i = 0; i < PAGE_SIZE; i++) {
            boolean visible = i < itemCount;
            connectorGlowLines[i].setVisible(visible);
            connectorLines[i].setVisible(visible);
            anchorNodes[i].setVisible(visible);
            if (!visible) {
                continue;
            }

            double angle = Math.toRadians(startAngle + i * angleStep);
            double itemCenterX = centerX + Math.cos(angle) * itemRadius;
            double itemCenterY = centerY + Math.sin(angle) * itemRadius;
            OrbitItemView item = visibleItems.get(i);
            item.setPrefSize(itemWidth, itemHeight);
            item.setMinSize(itemWidth, itemHeight);
            item.setMaxSize(itemWidth, itemHeight);
            item.resizeRelocate(
                    itemCenterX - itemWidth / 2.0,
                    itemCenterY - itemHeight / 2.0,
                    itemWidth,
                    itemHeight
            );

            double startX = centerX + Math.cos(angle) * connectorStartRadius;
            double startY = centerY + Math.sin(angle) * connectorStartRadius;
            double endRadius = itemRadius - Math.min(itemWidth, itemHeight) * 0.52;
            double endX = centerX + Math.cos(angle) * endRadius;
            double endY = centerY + Math.sin(angle) * endRadius;
            setLine(connectorGlowLines[i], startX, startY, endX, endY);
            setLine(connectorLines[i], startX, startY, endX, endY);
            anchorNodes[i].setCenterX(endX);
            anchorNodes[i].setCenterY(endY);
            anchorNodes[i].setRadius(clamp(base * 0.006, 3.3, 5.2));
        }

        double breadcrumbWidth = Math.min(width * 0.76, 720);
        breadcrumbLabel.resizeRelocate(
                centerX - breadcrumbWidth / 2.0,
                12,
                breadcrumbWidth,
                28
        );

        double pageY = Math.min(height - toolbarHeight - 66, centerY + radius + itemHeight * 0.57);
        double pageButtonSize = 42;
        previousPageButton.resizeRelocate(
                centerX - 82,
                pageY,
                pageButtonSize,
                pageButtonSize
        );
        nextPageButton.resizeRelocate(
                centerX + 40,
                pageY,
                pageButtonSize,
                pageButtonSize
        );
        pageIndicator.resizeRelocate(
                centerX - 37,
                pageY + 7,
                74,
                28
        );

        double toolbarWidth = Math.min(width - 24, Math.max(560, toolBar.prefWidth(-1)));
        toolBar.resizeRelocate(
                centerX - toolbarWidth / 2.0,
                height - toolbarHeight - 10,
                toolbarWidth,
                toolbarHeight
        );

        if (statusLabel.isVisible()) {
            double statusWidth = Math.min(width * 0.72, 560);
            statusLabel.resizeRelocate(
                    centerX - statusWidth / 2.0,
                    height - toolbarHeight - 43,
                    statusWidth,
                    26
            );
        }
    }

    private void layoutTicks(double centerX, double centerY, double radius) {
        for (int i = 0; i < tickLines.length; i++) {
            double angle = Math.toRadians(-90 + i * (360.0 / TICK_COUNT));
            double inner = radius * (i % 4 == 0 ? 0.94 : 0.965);
            double outer = radius * 1.035;
            setLine(
                    tickLines[i],
                    centerX + Math.cos(angle) * inner,
                    centerY + Math.sin(angle) * inner,
                    centerX + Math.cos(angle) * outer,
                    centerY + Math.sin(angle) * outer
            );
        }
    }

    private Circle createCircle(String styleClass) {
        Circle circle = new Circle();
        circle.getStyleClass().add(styleClass);
        circle.setMouseTransparent(true);
        return circle;
    }

    private Circle createRing(String styleClass) {
        Circle circle = createCircle("orbit-ring");
        circle.getStyleClass().add(styleClass);
        circle.setFill(Color.TRANSPARENT);
        return circle;
    }

    private Button createPageButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("orbit-page-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setFocusTraversable(true);
        return button;
    }

    private Button createToolButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("orbit-tool-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setFocusTraversable(true);
        return button;
    }

    private void setRing(Circle circle, double centerX, double centerY, double radius) {
        circle.setCenterX(centerX);
        circle.setCenterY(centerY);
        circle.setRadius(radius);
    }

    private void setLine(
            Line line,
            double startX,
            double startY,
            double endX,
            double endY
    ) {
        line.setStartX(startX);
        line.setStartY(startY);
        line.setEndX(endX);
        line.setEndY(endY);
    }

    private void consumeSecondaryClicks(Node node) {
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

    private List<OrbitEntry> safeCopy(List<OrbitEntry> entries) {
        return entries == null ? List.of() : List.copyOf(entries);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
