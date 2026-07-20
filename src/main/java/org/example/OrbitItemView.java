package org.example;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * One static preview item around the command crystal.
 */
public final class OrbitItemView extends StackPane {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final String title;
    private final String description;

    public OrbitItemView(String title, String icon, String description) {
        this.title = title;
        this.description = description;

        getStyleClass().add("orbit-item");
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefSize(112, 96);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setFocusTraversable(true);

        Label iconLabel = new Label(icon == null || icon.isBlank() ? "◇" : icon);
        iconLabel.getStyleClass().add("orbit-item__icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("orbit-item__title");
        titleLabel.setMaxWidth(96);
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        VBox content = new VBox(5, iconLabel, titleLabel);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);
        getChildren().add(content);

        Tooltip.install(this, new Tooltip(title + "\n" + description));

        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }
}
