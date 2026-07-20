package org.example;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven circular menu with trigonometric layout, paging and nested
 * navigation. Database integration and editable categories are later stages.
 */
public final class CircularMenuPane extends Pane {

    public static final int PAGE_SIZE = 5;

    private final Circle outerRing = createRing(0.62, 1.4);
    private final Circle middleRing = createRing(0.48, 1.0);
    private final Circle innerRing = createRing(0.34, 0.8);
    private final Line[] connectorLines = new Line[PAGE_SIZE];
    private final CommandCrystal commandCrystal = new CommandCrystal();
    private final Button previousPageButton = createPageButton("‹", "Предыдущая страница");
    private final Button nextPageButton = createPageButton("›", "Следующая страница");
    private final Label pageIndicator = new Label();
    private final Label breadcrumbLabel = new Label();
    private final OrbitNavigationStack navigation;
    private final List<OrbitItemView> visibleItems = new ArrayList<>();

    private int pageIndex;
    private OrbitItemView selectedItem;

    public CircularMenuPane(List<OrbitEntry> rootEntries) {
        getStyleClass().add("circular-menu-pane");
        setPickOnBounds(false);
        setFocusTraversable(true);

        navigation = new OrbitNavigationStack(rootEntries);
        pageIndicator.getStyleClass().add("orbit-page-indicator");
        breadcrumbLabel.getStyleClass().add("orbit-breadcrumb");
        breadcrumbLabel.setAlignment(Pos.CENTER);
        breadcrumbLabel.setMouseTransparent(true);

        getChildren().addAll(outerRing, middleRing, innerRing);

        for (int i = 0; i < connectorLines.length; i++) {
            Line line = new Line();
            line.setStroke(Color.rgb(72, 196, 255, 0.28));
            line.setStrokeWidth(1.0);
            line.setMouseTransparent(true);
            connectorLines[i] = line;
            getChildren().add(line);
        }

        getChildren().addAll(
                commandCrystal,
                previousPageButton,
                nextPageButton,
                pageIndicator,
                breadcrumbLabel
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

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.PAGE_UP && !previousPageButton.isDisabled()) {
                showPreviousPage();
                event.consume();
            } else if (event.getCode() == KeyCode.PAGE_DOWN && !nextPageButton.isDisabled()) {
                showNextPage();
                event.consume();
            }
        });

        refreshCurrentLevel();
    }

    public boolean handleEscape() {
        if (!navigation.canGoBack()) {
            return false;
        }
        navigateBack();
        return true;
    }

    public void resetNavigation() {
        navigation.reset();
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

    private void activate(OrbitItemView item) {
        OrbitEntry entry = item.getEntry();

        if (entry.isCategory() && entry.hasChildren()) {
            navigation.enter(entry);
            pageIndex = 0;
            clearSelection();
            refreshCurrentLevel();
            requestFocus();
            System.out.println("[Orbit] Открыта категория: " + entry.getTitle());
            return;
        }

        select(item);
        String subtitle = entry.isCategory()
                ? "Категория пока пуста"
                : entry.getDescription();
        commandCrystal.setSelectionText(entry.getTitle(), subtitle);
        commandCrystal.setBackAvailable(navigation.canGoBack());
        System.out.println("[Orbit] Выбран элемент: " + entry.getTitle());
    }

    private void select(OrbitItemView item) {
        clearSelection();
        selectedItem = item;
        selectedItem.setSelected(true);
    }

    private void clearSelection() {
        if (selectedItem != null) {
            selectedItem.setSelected(false);
            selectedItem = null;
        }
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

    private void showPreviousPage() {
        if (pageIndex <= 0) {
            return;
        }
        pageIndex--;
        clearSelection();
        refreshVisibleItems();
        updateCrystalForCurrentLevel();
    }

    private void showNextPage() {
        int pageCount = getPageCount();
        if (pageIndex >= pageCount - 1) {
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
                    activate(item);
                    event.consume();
                }
            });
            item.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    activate(item);
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
        String subtitle = navigation.isAtRoot()
                ? "Корень категорий"
                : navigation.getBreadcrumb();
        commandCrystal.setLevelText(title, subtitle, navigation.canGoBack());
    }

    private void updatePagingControls() {
        int pageCount = getPageCount();
        previousPageButton.setDisable(pageIndex <= 0);
        nextPageButton.setDisable(pageIndex >= pageCount - 1);
        previousPageButton.setVisible(pageCount > 1);
        nextPageButton.setVisible(pageCount > 1);
        pageIndicator.setVisible(pageCount > 1);
        pageIndicator.setText((pageIndex + 1) + " / " + pageCount);
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
        double centerY = height / 2.0;
        double radius = clamp(size * 0.36, 145, size * 0.41);
        double itemWidth = clamp(size * 0.15, 84, 124);
        double itemHeight = clamp(size * 0.13, 74, 106);
        double crystalSize = clamp(size * 0.42, 190, 360);

        layoutRing(outerRing, centerX, centerY, size * 0.46);
        layoutRing(middleRing, centerX, centerY, size * 0.365);
        layoutRing(innerRing, centerX, centerY, size * 0.255);

        commandCrystal.resizeRelocate(
                centerX - crystalSize / 2.0,
                centerY - crystalSize / 2.0,
                crystalSize,
                crystalSize
        );

        int itemCount = visibleItems.size();
        double angleStep = itemCount == 0 ? 0 : 360.0 / itemCount;
        double startAngle = -90.0;

        for (int i = 0; i < connectorLines.length; i++) {
            connectorLines[i].setVisible(i < itemCount);
        }

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

            Line line = connectorLines[i];
            double lineStartRadius = crystalSize * 0.32;
            double lineEndRadius = radius - Math.min(itemWidth, itemHeight) * 0.46;
            line.setStartX(centerX + directionX * lineStartRadius);
            line.setStartY(centerY + directionY * lineStartRadius);
            line.setEndX(centerX + directionX * lineEndRadius);
            line.setEndY(centerY + directionY * lineEndRadius);
        }

        double pageButtonSize = clamp(size * 0.07, 38, 54);
        previousPageButton.resizeRelocate(
                centerX - radius - pageButtonSize * 1.55,
                centerY - pageButtonSize / 2.0,
                pageButtonSize,
                pageButtonSize
        );
        nextPageButton.resizeRelocate(
                centerX + radius + pageButtonSize * 0.55,
                centerY - pageButtonSize / 2.0,
                pageButtonSize,
                pageButtonSize
        );

        pageIndicator.autosize();
        pageIndicator.relocate(
                centerX - pageIndicator.getWidth() / 2.0,
                centerY + radius + itemHeight * 0.72
        );

        breadcrumbLabel.resizeRelocate(
                Math.max(0, centerX - crystalSize * 0.9),
                Math.max(0, centerY - radius - itemHeight * 0.95),
                crystalSize * 1.8,
                28
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

    private Circle createRing(double opacity, double width) {
        Circle ring = new Circle();
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(48, 167, 255, opacity));
        ring.setStrokeWidth(width);
        ring.setMouseTransparent(true);
        return ring;
    }

    private void layoutRing(Circle ring, double centerX, double centerY, double radius) {
        ring.setCenterX(centerX);
        ring.setCenterY(centerY);
        ring.setRadius(radius);
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
