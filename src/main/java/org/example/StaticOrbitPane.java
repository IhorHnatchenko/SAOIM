package org.example;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.List;

/**
 * Responsive, intentionally static command-circle preview for step 3.
 *
 * The five normalized anchor positions are fixed. Generic trigonometric
 * distribution, paging and navigation belong to CircularMenuPane in step 4.
 */
public final class StaticOrbitPane extends Pane {

    private static final double[][] STATIC_ANCHORS = {
            {0.00, -1.00},
            {0.95, -0.30},
            {0.60, 0.82},
            {-0.60, 0.82},
            {-0.95, -0.30}
    };

    private final Circle outerRing = createRing(0.62, 1.4);
    private final Circle middleRing = createRing(0.48, 1.0);
    private final Circle innerRing = createRing(0.34, 0.8);
    private final Line[] connectorLines = new Line[5];
    private final CommandCrystal commandCrystal = new CommandCrystal();
    private final List<OrbitItemView> items;
    private OrbitItemView selectedItem;

    public StaticOrbitPane() {
        getStyleClass().add("static-orbit-pane");
        setPickOnBounds(false);

        items = List.of(
                new OrbitItemView("Работа", "▣", "Тестовая категория для рабочих инструментов"),
                new OrbitItemView("Медиа", "▷", "Тестовая категория для видео и музыки"),
                new OrbitItemView("Система", "⬡", "Тестовая категория системных функций"),
                new OrbitItemView("Обучение", "◇", "Тестовая категория учебных ресурсов"),
                new OrbitItemView("Игры", "✦", "Тестовая категория игр")
        );

        getChildren().addAll(outerRing, middleRing, innerRing);
        for (int i = 0; i < connectorLines.length; i++) {
            Line line = new Line();
            line.setStroke(Color.rgb(72, 196, 255, 0.28));
            line.setStrokeWidth(1.0);
            connectorLines[i] = line;
            getChildren().add(line);
        }
        getChildren().add(commandCrystal);
        getChildren().addAll(items);

        for (OrbitItemView item : items) {
            item.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    select(item);
                    event.consume();
                }
            });
            item.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    select(item);
                    event.consume();
                }
            });
        }

        commandCrystal.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        commandCrystal.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double size = Math.max(320, Math.min(width, height));
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double radius = size * 0.36;
        double itemWidth = clamp(size * 0.15, 82, 122);
        double itemHeight = clamp(size * 0.13, 72, 104);
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

        for (int i = 0; i < items.size(); i++) {
            double anchorX = STATIC_ANCHORS[i][0];
            double anchorY = STATIC_ANCHORS[i][1];
            double itemCenterX = centerX + radius * anchorX;
            double itemCenterY = centerY + radius * anchorY;

            OrbitItemView item = items.get(i);
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
            double vectorLength = Math.hypot(anchorX, anchorY);
            double normalizedX = anchorX / vectorLength;
            double normalizedY = anchorY / vectorLength;
            double lineStartRadius = crystalSize * 0.32;
            double lineEndRadius = radius - Math.min(itemWidth, itemHeight) * 0.44;
            line.setStartX(centerX + normalizedX * lineStartRadius);
            line.setStartY(centerY + normalizedY * lineStartRadius);
            line.setEndX(centerX + normalizedX * lineEndRadius);
            line.setEndY(centerY + normalizedY * lineEndRadius);
        }
    }

    private void select(OrbitItemView item) {
        if (selectedItem != null) {
            selectedItem.setSelected(false);
        }
        selectedItem = item;
        selectedItem.setSelected(true);
        commandCrystal.setSelectionText(item.getTitle(), "Тестовая категория — этап 3");
        System.out.println("[Profile] Выбран тестовый элемент: " + item.getTitle());
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
