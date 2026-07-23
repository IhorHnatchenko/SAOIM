package org.example;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final Label fallbackIconLabel = new Label();
    private final ImageView imageView = new ImageView();
    private final Label markerLabel = new Label();
    private final Tooltip tooltip = new Tooltip();
    private final Runnable onAvailabilityChanged;
    private LaunchAvailability availability;

    public OrbitItemView(OrbitEntry entry) {
        this(entry, null, null, null, null);
    }

    public OrbitItemView(
            OrbitEntry entry,
            AppShortcut shortcut,
            ShortcutAvailabilityService availabilityService,
            IconService iconService
    ) {
        this(entry, shortcut, availabilityService, iconService, null);
    }

    public OrbitItemView(
            OrbitEntry entry,
            AppShortcut shortcut,
            ShortcutAvailabilityService availabilityService,
            IconService iconService,
            Runnable onAvailabilityChanged
    ) {
        this.entry = entry == null
                ? OrbitEntry.item("empty", "Без названия", "◇", "Нет описания")
                : entry;
        this.onAvailabilityChanged = onAvailabilityChanged == null
                ? () -> { }
                : onAvailabilityChanged;
        this.availability = initialAvailability(this.entry, shortcut, availabilityService);

        getStyleClass().add("orbit-item");
        pseudoClassStateChanged(CATEGORY, this.entry.isCategory());
        pseudoClassStateChanged(LEAF, !this.entry.isCategory());
        pseudoClassStateChanged(PINNED, this.entry.isRootPinned());
        pseudoClassStateChanged(SHORTCUT, this.entry.isShortcut());

        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefSize(112, 96);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.BUTTON);
        setAccessibleText(this.entry.getTitle());

        fallbackIconLabel.setText(this.entry.getIcon());
        fallbackIconLabel.getStyleClass().add("orbit-item__icon");

        imageView.getStyleClass().add("orbit-item__image");
        imageView.setFitWidth(42);
        imageView.setFitHeight(42);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setVisible(false);
        imageView.setManaged(false);

        StackPane iconSlot = new StackPane(fallbackIconLabel, imageView);
        iconSlot.getStyleClass().add("orbit-item__icon-slot");
        iconSlot.setMouseTransparent(true);

        Label titleLabel = new Label(this.entry.getTitle());
        titleLabel.getStyleClass().add("orbit-item__title");
        titleLabel.setMaxWidth(100);
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        markerLabel.getStyleClass().add("orbit-item__marker");

        VBox content = new VBox(4, iconSlot, titleLabel, markerLabel);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);
        getChildren().add(content);

        Tooltip.install(this, tooltip);
        updateAvailabilityPresentation();
        consumeSecondaryClicks();

        if (this.entry.isShortcut() && shortcut != null) {
            if (availabilityService != null && this.entry.isEnabled()) {
                availabilityService.checkAsync(shortcut, this::setAvailability);
            }
            if (iconService != null) {
                iconService.loadIconAsync(shortcut, this::showImage);
            }
        }
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

    public LaunchAvailability getAvailability() {
        return availability;
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }

    private LaunchAvailability initialAvailability(
            OrbitEntry entry,
            AppShortcut shortcut,
            ShortcutAvailabilityService service
    ) {
        if (!entry.isShortcut()) {
            return LaunchAvailability.available("");
        }
        if (!entry.isEnabled()) {
            return LaunchAvailability.unavailable("Ярлык отключён пользователем.");
        }
        if (shortcut == null || service == null) {
            return LaunchAvailability.unavailable("Данные ярлыка пока недоступны.");
        }
        return LaunchAvailability.checking("Проверка доступности…");
    }

    private void setAvailability(LaunchAvailability newAvailability) {
        availability = newAvailability == null
                ? LaunchAvailability.unavailable("Не удалось определить доступность ярлыка.")
                : newAvailability;
        updateAvailabilityPresentation();
        onAvailabilityChanged.run();
    }

    private void updateAvailabilityPresentation() {
        boolean unavailable = entry.isShortcut()
                && !availability.available()
                && !availability.checking();
        pseudoClassStateChanged(UNAVAILABLE, unavailable);
        markerLabel.setText(markerText(entry, availability));
        tooltip.setText(tooltipText(entry, availability));
    }

    private void showImage(Image image) {
        if (image == null || image.isError()) {
            return;
        }
        imageView.setImage(image);
        imageView.setVisible(true);
        imageView.setManaged(true);
        fallbackIconLabel.setVisible(false);
        fallbackIconLabel.setManaged(false);
    }

    private String markerText(OrbitEntry entry, LaunchAvailability availability) {
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
            if (availability.checking()) {
                return "ПРОВЕРКА…";
            }
            if (!availability.available()) {
                return "НЕДОСТУПЕН";
            }
            return "▶ ЗАПУСТИТЬ";
        }
        return "ВЫБРАТЬ";
    }

    private String tooltipText(OrbitEntry entry, LaunchAvailability availability) {
        StringBuilder builder = new StringBuilder()
                .append(entry.getTitle())
                .append('\n')
                .append(entry.getDescription());
        if (entry.isCategory()) {
            builder.append("\nДвойной клик или Enter — открыть");
        } else if (entry.isShortcut()) {
            if (!availability.message().isBlank()) {
                builder.append("\n").append(availability.message());
            }
            if (availability.available()) {
                builder.append("\nДвойной клик или Enter — запустить");
            }
            builder.append("\nF2 — изменить, Delete — удалить");
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
