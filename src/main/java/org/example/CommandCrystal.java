package org.example;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.TextAlignment;

/**
 * Central command crystal. It also acts as the Back button for orbit navigation.
 */
public final class CommandCrystal extends StackPane {

    private static final PseudoClass BACK_AVAILABLE =
            PseudoClass.getPseudoClass("back-available");

    private final Pane artwork = new Pane();
    private final StackPane textLayer = new StackPane();
    private final VBox labels = new VBox(5);

    private final Circle glow = new Circle();
    private final Circle core = new Circle();
    private final Polygon outerHex = new Polygon();
    private final Polygon innerDiamond = new Polygon();
    private final Line[] spokes = new Line[6];
    private final Label titleLabel = new Label("Центр управления");
    private final Label subtitleLabel = new Label("Корень категорий");

    private boolean backAvailable;

    public CommandCrystal() {
        getStyleClass().add("command-crystal");
        setMinSize(150, 150);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText("Центральный кристалл");

        glow.getStyleClass().add("command-crystal__glow");
        core.getStyleClass().add("command-crystal__core");
        outerHex.getStyleClass().add("command-crystal__outer");
        innerDiamond.getStyleClass().add("command-crystal__inner");

        artwork.setMouseTransparent(true);
        artwork.getChildren().addAll(glow, core, outerHex, innerDiamond);
        for (int i = 0; i < spokes.length; i++) {
            Line spoke = new Line();
            spoke.setStroke(Color.rgb(77, 213, 255, 0.48));
            spoke.setStrokeWidth(1.1);
            spoke.setMouseTransparent(true);
            spokes[i] = spoke;
            artwork.getChildren().add(spoke);
        }

        configureCenteredLabel(titleLabel, "command-crystal__title");
        configureCenteredLabel(subtitleLabel, "command-crystal__subtitle");

        labels.getChildren().addAll(titleLabel, subtitleLabel);
        labels.setAlignment(Pos.CENTER);
        labels.setFillWidth(true);
        labels.setMouseTransparent(true);

        textLayer.getStyleClass().add("command-crystal__text-layer");
        textLayer.setAlignment(Pos.CENTER);
        textLayer.setMouseTransparent(true);
        textLayer.getChildren().add(labels);

        getChildren().addAll(artwork, textLayer);
        setAlignment(Pos.CENTER);
    }

    public void setLevelText(String title, String breadcrumb, boolean canGoBack) {
        titleLabel.setText(normalize(title, "Центр управления"));
        subtitleLabel.setText(
                canGoBack
                        ? "Нажмите для возврата\n" + normalize(breadcrumb, "Категории")
                        : normalize(breadcrumb, "Корень категорий")
        );
        setBackAvailable(canGoBack);
    }

    public void setSelectionText(String title, String subtitle) {
        titleLabel.setText(normalize(title, "Центр управления"));
        subtitleLabel.setText(normalize(subtitle, "Ядро системы"));
    }

    public boolean isBackAvailable() {
        return backAvailable;
    }

    public void setBackAvailable(boolean backAvailable) {
        this.backAvailable = backAvailable;
        pseudoClassStateChanged(BACK_AVAILABLE, backAvailable);
        setCursor(backAvailable ? Cursor.HAND : Cursor.DEFAULT);
        setAccessibleText(backAvailable
                ? "Центральный кристалл. Вернуться на уровень выше"
                : "Центральный кристалл");
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        double width = getWidth();
        double height = getHeight();
        double size = Math.max(1, Math.min(width, height));
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double outerRadius = size * 0.34;
        double coreRadius = size * 0.115;

        artwork.resizeRelocate(0, 0, width, height);

        glow.setCenterX(centerX);
        glow.setCenterY(centerY);
        glow.setRadius(size * 0.43);

        core.setCenterX(centerX);
        core.setCenterY(centerY);
        core.setRadius(coreRadius);

        setRegularPolygon(outerHex, centerX, centerY, outerRadius, 6, -90);
        setDiamond(innerDiamond, centerX, centerY, size * 0.22, size * 0.27);

        for (int i = 0; i < spokes.length; i++) {
            double angle = Math.toRadians(-90 + i * 60.0);
            Line spoke = spokes[i];
            spoke.setStartX(centerX);
            spoke.setStartY(centerY);
            spoke.setEndX(centerX + Math.cos(angle) * outerRadius);
            spoke.setEndY(centerY + Math.sin(angle) * outerRadius);
        }

        // The text layer is explicitly positioned relative to this crystal,
        // not relative to the monitor or parent ProfileView.
        double textWidth = size * 0.62;
        double textHeight = size * 0.44;
        textLayer.resizeRelocate(
                centerX - textWidth / 2.0,
                centerY - textHeight / 2.0,
                textWidth,
                textHeight
        );
        labels.setPrefWidth(textWidth);
        labels.setMaxWidth(textWidth);
        titleLabel.setMaxWidth(textWidth);
        subtitleLabel.setMaxWidth(textWidth);
    }

    private void configureCenteredLabel(Label label, String styleClass) {
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    private void setRegularPolygon(
            Polygon polygon,
            double centerX,
            double centerY,
            double radius,
            int sides,
            double startAngleDegrees
    ) {
        polygon.getPoints().clear();
        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(startAngleDegrees + (360.0 / sides) * i);
            polygon.getPoints().addAll(
                    centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius
            );
        }
    }

    private void setDiamond(
            Polygon polygon,
            double centerX,
            double centerY,
            double halfWidth,
            double halfHeight
    ) {
        polygon.getPoints().setAll(
                centerX, centerY - halfHeight,
                centerX + halfWidth, centerY,
                centerX, centerY + halfHeight,
                centerX - halfWidth, centerY
        );
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
