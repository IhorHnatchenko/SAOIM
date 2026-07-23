package org.example;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** One interactive category or shortcut on the circular orbit. */
public final class OrbitItemView extends StackPane {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CATEGORY = PseudoClass.getPseudoClass("category");
    private static final PseudoClass LEAF = PseudoClass.getPseudoClass("leaf");
    private static final PseudoClass PINNED = PseudoClass.getPseudoClass("pinned");
    private static final PseudoClass SHORTCUT = PseudoClass.getPseudoClass("shortcut");
    private static final PseudoClass UNAVAILABLE = PseudoClass.getPseudoClass("unavailable");

    private final OrbitEntry entry;

    public OrbitItemView(OrbitEntry entry) {
        this.entry = entry == null
                ? OrbitEntry.item("empty", "Без названия", "◇", "Нет описания")
                : entry;

        getStyleClass().add("orbit-item");
        pseudoClassStateChanged(CATEGORY, this.entry.isCategory());
        pseudoClassStateChanged(LEAF, !this.entry.isCategory());
        pseudoClassStateChanged(PINNED, this.entry.isRootPinned());
        pseudoClassStateChanged(SHORTCUT, this.entry.isShortcut());
        pseudoClassStateChanged(UNAVAILABLE, this.entry.isShortcut() && !this.entry.isEnabled());

        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefSize(112, 96);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(this.entry.getTitle());

        Label iconLabel = new Label(this.entry.getIcon());
        iconLabel.getStyleClass().add("orbit-item__icon");

        Label titleLabel = new Label(this.entry.getTitle());
        titleLabel.getStyleClass().add("orbit-item__title");
        titleLabel.setMaxWidth(100);
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label markerLabel = new Label(markerText(this.entry));
        markerLabel.getStyleClass().add("orbit-item__marker");

        VBox content = new VBox(4, iconLabel, titleLabel, markerLabel);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);
        getChildren().add(content);

        Tooltip.install(this, new Tooltip(tooltipText(this.entry)));
        consumeSecondaryClicks();
    }

    /** Compatibility constructor for old StaticOrbitPane. */
    public OrbitItemView(String title, String icon, String description) {
        this(OrbitEntry.item(title, title, icon, description));
    }

    public OrbitEntry getEntry() {
        return entry;
    }

    public String getTitle() {
        return entry.getTitle();
    }

    public String getDescription() {
        return entry.getDescription();
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }

    private String markerText(OrbitEntry entry) {
        if (entry.isRootPinned()) {
            return "★ ЗАКРЕПЛЕНА";
        }
        if (entry.isCategory()) {
            return "ДВОЙНОЙ КЛИК";
        }
        if (entry.isShortcut()) {
            if (!entry.isEnabled()) {
                return "ОТКЛЮЧЁН";
            }
            LaunchType type = entry.getLaunchType();
            return type == null ? "ЯРЛЫК" : type.name();
        }
        return "ВЫБРАТЬ";
    }

    private String tooltipText(OrbitEntry entry) {
        StringBuilder builder = new StringBuilder()
                .append(entry.getTitle())
                .append('\n')
                .append(entry.getDescription());
        if (entry.isCategory()) {
            builder.append("\nДвойной клик или Enter — открыть");
        } else if (entry.isShortcut()) {
            builder.append("\nЗапуск будет подключён на этапе 8")
                    .append("\nF2 — изменить, Delete — удалить");
        }
        return builder.toString();
    }

    private void consumeSecondaryClicks() {
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
}
