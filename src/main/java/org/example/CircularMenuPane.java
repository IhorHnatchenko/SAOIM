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

/**
 * Data-driven circular menu with paging, nested navigation and category
 * management controls.
 */
public final class CircularMenuPane extends Pane {
    public static final int PAGE_SIZE = 8;
    private static final int TICK_COUNT = 32;

    public interface CategoryActions {
        void createCategory(Long parentCategoryId, String parentLabel);

        void editCategory(long categoryId);

        void moveCategory(long categoryId);

        void moveCategoryUp(long categoryId);

        void moveCategoryDown(long categoryId);

        void togglePinned(long categoryId);

        void deleteCategory(long categoryId);

        void refreshCategories();
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

    private final Button showAllButton = createToolButton("Все", "Показать все корневые категории");
    private final Button createButton = createToolButton("＋", "Создать категорию (Insert)");
    private final Button editButton = createToolButton("✎", "Переименовать и изменить значок (F2)");
    private final Button moveButton = createToolButton("⇄", "Переместить категорию");
    private final Button upButton = createToolButton("↑", "Поднять выше (Ctrl+Up)");
    private final Button downButton = createToolButton("↓", "Опустить ниже (Ctrl+Down)");
    private final Button pinButton = createToolButton("★", "Закрепить или открепить корневую категорию");
    private final Button deleteButton = createToolButton("⌫", "Удалить категорию (Delete)");
    private final Button refreshButton = createToolButton("↻", "Обновить категории");
    private final HBox toolBar = new HBox(
            7,
            showAllButton,
            createButton,
            editButton,
            moveButton,
            upButton,
            downButton,
            pinButton,
            deleteButton,
            refreshButton
    );

    private final OrbitNavigationStack navigation;
    private final List<OrbitItemView> visibleItems = new ArrayList<>();

    private List<OrbitEntry> pinnedRootEntries = List.of();
    private List<OrbitEntry> allRootEntries = List.of();
    private CategoryActions categoryActions;
    private int pageIndex;
    private OrbitItemView selectedItem;
    private boolean showAllRootCategories;
    private boolean busy;

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
        createButton.setOnAction(event -> createCategory());
        editButton.setOnAction(event -> withSelectedCategory(categoryActions::editCategory));
        moveButton.setOnAction(event -> withSelectedCategory(categoryActions::moveCategory));
        upButton.setOnAction(event -> withSelectedCategory(categoryActions::moveCategoryUp));
        downButton.setOnAction(event -> withSelectedCategory(categoryActions::moveCategoryDown));
        pinButton.setOnAction(event -> withSelectedCategory(categoryActions::togglePinned));
        deleteButton.setOnAction(event -> withSelectedCategory(categoryActions::deleteCategory));
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
        return navigation.isAtRoot() ? "Корень категорий" : navigation.getBreadcrumb();
    }

    public void setBusy(boolean busy, String message) {
        this.busy = busy;
        if (message != null) {
            setStatus(message);
        }
        updateToolBarState();
    }

    public void setStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setVisible(message != null && !message.isBlank());
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
        showAllRootCategories = false;
        if (pinnedRootEntries.isEmpty()) {
            showAllRootCategories = true;
        }
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
        } else if (event.getCode() == KeyCode.INSERT) {
            createCategory();
            event.consume();
        } else if (event.getCode() == KeyCode.F2) {
            withSelectedCategory(categoryActions == null ? null : categoryActions::editCategory);
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE) {
            withSelectedCategory(categoryActions == null ? null : categoryActions::deleteCategory);
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.UP) {
            withSelectedCategory(categoryActions == null ? null : categoryActions::moveCategoryUp);
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.DOWN) {
            withSelectedCategory(categoryActions == null ? null : categoryActions::moveCategoryDown);
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.P) {
            withSelectedCategory(categoryActions == null ? null : categoryActions::togglePinned);
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
        commandCrystal.setSelectionText(
                entry.getTitle(),
                entry.isRootPinned() ? "Закреплена на главной орбите" : entry.getDescription()
        );
        commandCrystal.setBackAvailable(navigation.canGoBack());
        updateToolBarState();
    }

    private void open(OrbitItemView item) {
        OrbitEntry entry = item.getEntry();
        if (!entry.isCategory()) {
            select(item);
            return;
        }
        navigation.enter(entry);
        pageIndex = 0;
        clearSelection();
        refreshCurrentLevel();
        requestFocus();
        System.out.println("[Orbit] Открыта категория: " + entry.getTitle());
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
            setStatus("Нет закреплённых категорий. Выберите категорию и нажмите ★.");
        }
        navigation.replaceRootEntries(getActiveRootEntries(), List.of());
        pageIndex = 0;
        clearSelection();
        refreshCurrentLevel();
    }

    private List<OrbitEntry> getActiveRootEntries() {
        return showAllRootCategories ? allRootEntries : pinnedRootEntries;
    }

    private void createCategory() {
        if (categoryActions == null || busy) {
            return;
        }
        categoryActions.createCategory(
                navigation.getCurrentCategoryId(),
                navigation.isAtRoot() ? "Корень категорий" : navigation.getBreadcrumb()
        );
    }

    private void withSelectedCategory(java.util.function.LongConsumer action) {
        if (action == null || busy || selectedItem == null) {
            return;
        }
        Long categoryId = selectedItem.getEntry().getCategoryId();
        if (categoryId != null && categoryId > 0) {
            action.accept(categoryId);
        }
    }

    private void showPreviousPage() {
        if (pageIndex <= 0 || busy) {
            return;
        }
        pageIndex--;
        clearSelection();
        refreshVisibleItems();
        updateCrystalForCurrentLevel();
    }

    private void showNextPage() {
        int pageCount = getPageCount();
        if (pageIndex >= pageCount - 1 || busy) {
            return;
        }
        pageIndex++;
        clearSelection();
        refreshVisibleItems();
        updateCrystalForCurrentLevel();
    }

    private void refreshCurrentLevel() {
        pageIndex = clampPageIndex(pageIndex);
        refreshVisibleItems();
        updateCrystalForCurrentLevel();
        breadcrumbLabel.setText(navigation.getBreadcrumb());
        updateToolBarState();
    }

    private void refreshVisibleItems() {
        getChildren().removeAll(visibleItems);
        visibleItems.clear();

        List<OrbitEntry> entries = navigation.getCurrentEntries();
        pageIndex = clampPageIndex(pageIndex);
        int fromIndex = Math.min(pageIndex * PAGE_SIZE, entries.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, entries.size());

        for (OrbitEntry entry : entries.subList(fromIndex, toIndex)) {
            OrbitItemView item = new OrbitItemView(entry);
            item.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (event.getClickCount() >= 2) {
                        open(item);
                    } else {
                        select(item);
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
        }
        getChildren().addAll(visibleItems);
        updatePagingControls();
        requestLayout();
    }

    private void updateCrystalForCurrentLevel() {
        String title = navigation.getCurrentTitle();
        String subtitle;
        if (navigation.isAtRoot()) {
            subtitle = showAllRootCategories
                    ? "Все корневые категории"
                    : "Закреплённые категории";
        } else {
            subtitle = navigation.getBreadcrumb();
        }
        commandCrystal.setLevelText(title, subtitle, navigation.canGoBack());
    }

    private void updatePagingControls() {
        int pageCount = getPageCount();
        previousPageButton.setDisable(pageIndex <= 0 || busy);
        nextPageButton.setDisable(pageIndex >= pageCount - 1 || busy);
        previousPageButton.setVisible(pageCount > 1);
        nextPageButton.setVisible(pageCount > 1);
        pageIndicator.setVisible(pageCount > 1);
        pageIndicator.setText((pageIndex + 1) + " / " + pageCount);
    }

    private void updateToolBarState() {
        boolean actionsAvailable = categoryActions != null && !busy;
        OrbitEntry selected = getSelectedEntry();
        boolean hasCategory = actionsAvailable
                && selected != null
                && selected.isDatabaseCategory();
        boolean rootSelected = hasCategory
                && allRootEntries.stream().anyMatch(entry -> selected.getCategoryId().equals(
                entry.getCategoryId()
        ));

        createButton.setDisable(!actionsAvailable);
        editButton.setDisable(!hasCategory);
        moveButton.setDisable(!hasCategory);
        upButton.setDisable(!hasCategory);
        downButton.setDisable(!hasCategory);
        deleteButton.setDisable(!hasCategory);
        pinButton.setDisable(!rootSelected);
        refreshButton.setDisable(!actionsAvailable);
        showAllButton.setDisable(!actionsAvailable || !navigation.isAtRoot());
        showAllButton.setText(showAllRootCategories ? "★" : "Все");
        showAllButton.setAccessibleText(
                showAllRootCategories
                        ? "Показать только закреплённые категории"
                        : "Показать все корневые категории"
        );
        if (selected != null && selected.isRootPinned()) {
            pinButton.setText("☆");
            pinButton.setAccessibleText("Открепить корневую категорию");
        } else {
            pinButton.setText("★");
            pinButton.setAccessibleText("Закрепить корневую категорию");
        }
        updatePagingControls();
    }

    private int getPageCount() {
        int size = navigation.getCurrentEntries().size();
        return Math.max(1, (int) Math.ceil(size / (double) PAGE_SIZE));
    }

    private int clampPageIndex(int requestedIndex) {
        return Math.max(0, Math.min(requestedIndex, getPageCount() - 1));
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double size = Math.max(320, Math.min(width, height));
        double centerX = width / 2.0;
        double centerY = height / 2.0 - clamp(size * 0.025, 8, 22);
        double radius = clamp(size * 0.355, 150, size * 0.405);
        double itemWidth = clamp(size * 0.128, 80, 116);
        double itemHeight = clamp(size * 0.108, 70, 94);
        double crystalSize = clamp(size * 0.40, 190, 350);

        layoutCircle(orbitBackdrop, centerX, centerY, size * 0.455);
        layoutCircle(outerGlowRing, centerX, centerY, size * 0.438);
        layoutCircle(outerRing, centerX, centerY, size * 0.425);
        layoutCircle(middleRing, centerX, centerY, size * 0.340);
        layoutCircle(innerRing, centerX, centerY, size * 0.258);
        layoutCircle(coreRing, centerX, centerY, size * 0.178);
        layoutTicks(centerX, centerY, size * 0.402, size * 0.435);

        commandCrystal.resizeRelocate(
                centerX - crystalSize / 2.0,
                centerY - crystalSize / 2.0,
                crystalSize,
                crystalSize
        );

        int itemCount = visibleItems.size();
        double angleStep = itemCount == 0 ? 0 : 360.0 / itemCount;
        double startAngle = -90.0;
        updateConnectorVisibility(itemCount);

        for (int i = 0; i < itemCount; i++) {
            double angle = Math.toRadians(startAngle + i * angleStep);
            double directionX = Math.cos(angle);
            double directionY = Math.sin(angle);
            double itemCenterX = centerX + radius * directionX;
            double itemCenterY = centerY + radius * directionY;

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

            double lineStartRadius = crystalSize * 0.325;
            double lineEndRadius = radius - Math.min(itemWidth, itemHeight) * 0.52;
            double startX = centerX + directionX * lineStartRadius;
            double startY = centerY + directionY * lineStartRadius;
            double endX = centerX + directionX * lineEndRadius;
            double endY = centerY + directionY * lineEndRadius;
            layoutLine(connectorGlowLines[i], startX, startY, endX, endY);
            layoutLine(connectorLines[i], startX, startY, endX, endY);

            Circle anchor = anchorNodes[i];
            anchor.setCenterX(endX);
            anchor.setCenterY(endY);
            anchor.setRadius(clamp(size * 0.006, 3.2, 5.2));
        }

        layoutPagingControls(centerX, centerY, radius, itemHeight, size);
        breadcrumbLabel.resizeRelocate(
                Math.max(0, centerX - crystalSize),
                Math.max(0, centerY - radius - itemHeight * 0.95),
                crystalSize * 2.0,
                30
        );

        double toolbarWidth = Math.min(width - 30, 560);
        double toolbarHeight = 48;
        toolBar.resizeRelocate(
                centerX - toolbarWidth / 2.0,
                Math.min(height - toolbarHeight - 10, centerY + radius + itemHeight * 1.28),
                toolbarWidth,
                toolbarHeight
        );
        toolBar.layout();

        statusLabel.resizeRelocate(
                Math.max(10, centerX - Math.min(width * 0.38, 360)),
                Math.min(height - 27, toolBar.getLayoutY() + toolbarHeight + 2),
                Math.min(width * 0.76, 720),
                24
        );
    }

    private void layoutTicks(
            double centerX,
            double centerY,
            double innerRadius,
            double outerRadius
    ) {
        for (int i = 0; i < tickLines.length; i++) {
            double angle = Math.toRadians(-90.0 + i * (360.0 / tickLines.length));
            boolean major = i % 4 == 0;
            double tickInnerRadius = major ? innerRadius - 4.0 : innerRadius;
            double tickOuterRadius = major ? outerRadius + 3.0 : outerRadius;
            Line tick = tickLines[i];
            tick.setStartX(centerX + Math.cos(angle) * tickInnerRadius);
            tick.setStartY(centerY + Math.sin(angle) * tickInnerRadius);
            tick.setEndX(centerX + Math.cos(angle) * tickOuterRadius);
            tick.setEndY(centerY + Math.sin(angle) * tickOuterRadius);
        }
    }

    private void updateConnectorVisibility(int itemCount) {
        for (int i = 0; i < PAGE_SIZE; i++) {
            boolean visible = i < itemCount;
            connectorGlowLines[i].setVisible(visible);
            connectorLines[i].setVisible(visible);
            anchorNodes[i].setVisible(visible);
        }
    }

    private void layoutPagingControls(
            double centerX,
            double centerY,
            double radius,
            double itemHeight,
            double size
    ) {
        double pageButtonSize = clamp(size * 0.056, 36, 48);
        double controlsY = centerY + radius + itemHeight * 0.58;
        pageIndicator.autosize();
        double indicatorWidth = Math.max(58, pageIndicator.getWidth());
        double gap = 12;
        previousPageButton.resizeRelocate(
                centerX - indicatorWidth / 2.0 - gap - pageButtonSize,
                controlsY,
                pageButtonSize,
                pageButtonSize
        );
        nextPageButton.resizeRelocate(
                centerX + indicatorWidth / 2.0 + gap,
                controlsY,
                pageButtonSize,
                pageButtonSize
        );
        pageIndicator.autosize();
        pageIndicator.relocate(
                centerX - pageIndicator.getWidth() / 2.0,
                controlsY + pageButtonSize / 2.0 - pageIndicator.getHeight() / 2.0
        );
    }

    private Button createPageButton(String text, String accessibleText) {
        Button button = new Button(text);
        button.getStyleClass().add("orbit-page-button");
        button.setFocusTraversable(true);
        button.setAccessibleText(accessibleText);
        consumeSecondaryClicks(button);
        return button;
    }

    private Button createToolButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.getStyleClass().add("orbit-tool-button");
        button.setFocusTraversable(true);
        button.setAccessibleText(tooltipText);
        Tooltip.install(button, new Tooltip(tooltipText));
        consumeSecondaryClicks(button);
        return button;
    }

    private Circle createRing(String modifierStyleClass) {
        Circle ring = createCircle("orbit-ring");
        ring.getStyleClass().add(modifierStyleClass);
        return ring;
    }

    private Circle createCircle(String styleClass) {
        Circle circle = new Circle();
        circle.getStyleClass().add(styleClass);
        circle.setMouseTransparent(true);
        return circle;
    }

    private void layoutCircle(Circle circle, double centerX, double centerY, double radius) {
        circle.setCenterX(centerX);
        circle.setCenterY(centerY);
        circle.setRadius(radius);
    }

    private void layoutLine(Line line, double startX, double startY, double endX, double endY) {
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
