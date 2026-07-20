package org.example;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/**
 * Static central crystal for step 3. Navigation behavior is added in step 4.
 */
public final class CommandCrystal extends StackPane {

    private final Pane artwork = new Pane();
    private final Circle glow = new Circle();
    private final Circle core = new Circle();
    private final Polygon outerHex = new Polygon();
    private final Polygon innerDiamond = new Polygon();
    private final Line[] spokes = new Line[6];
    private final Label titleLabel = new Label("Центр управления");
    private final Label subtitleLabel = new Label("Ядро системы");

    public CommandCrystal() {
        getStyleClass().add("command-crystal");
        setMinSize(150, 150);

        glow.getStyleClass().add("command-crystal__glow");
        core.getStyleClass().add("command-crystal__core");
        outerHex.getStyleClass().add("command-crystal__outer");
        innerDiamond.getStyleClass().add("command-crystal__inner");

        artwork.getChildren().addAll(glow, core, outerHex, innerDiamond);
        for (int i = 0; i < spokes.length; i++) {
            Line spoke = new Line();
            spoke.setStroke(Color.rgb(77, 213, 255, 0.48));
            spoke.setStrokeWidth(1.1);
            spokes[i] = spoke;
            artwork.getChildren().add(spoke);
        }

        titleLabel.getStyleClass().add("command-crystal__title");
        subtitleLabel.getStyleClass().add("command-crystal__subtitle");
        VBox labels = new VBox(3, titleLabel, subtitleLabel);
        labels.setAlignment(Pos.CENTER);
        labels.setMouseTransparent(true);

        getChildren().addAll(artwork, labels);
        setAlignment(Pos.CENTER);
    }

    public void setSelectionText(String title, String subtitle) {
        titleLabel.setText(title == null || title.isBlank() ? "Центр управления" : title);
        subtitleLabel.setText(subtitle == null || subtitle.isBlank() ? "Ядро системы" : subtitle);
    }

    @Override
    protected void layoutChildren() {
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

        super.layoutChildren();
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

    private void setDiamond(Polygon polygon, double centerX, double centerY, double halfWidth, double halfHeight) {
        polygon.getPoints().setAll(
                centerX, centerY - halfHeight,
                centerX + halfWidth, centerY,
                centerX, centerY + halfHeight,
                centerX - halfWidth, centerY
        );
    }
}
