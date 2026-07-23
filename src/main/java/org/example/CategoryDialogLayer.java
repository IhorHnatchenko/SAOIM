package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Modal in-scene dialogs for categories and shortcuts. */
public final class CategoryDialogLayer extends StackPane {
    private final StackPane dimmer = new StackPane();
    private final VBox dialogCard = new VBox(12);

    public CategoryDialogLayer() {
        getStyleClass().add("category-dialog-layer");
        dimmer.getStyleClass().add("category-dialog-dimmer");
        dialogCard.getStyleClass().add("category-dialog-card");
        dialogCard.setAlignment(Pos.CENTER_LEFT);
        dialogCard.setPadding(new Insets(22));
        dialogCard.setMaxWidth(520);
        dialogCard.setMinWidth(380);
        dialogCard.setMaxHeight(760);

        setAlignment(Pos.CENTER);
        getChildren().addAll(dimmer, dialogCard);
        setVisible(false);
        setManaged(false);
        setPickOnBounds(true);

        dimmer.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                close();
                event.consume();
            }
        });
        dialogCard.setOnMouseClicked(event -> event.consume());
        dialogCard.setOnMousePressed(event -> event.consume());
        setOnContextMenuRequested(event -> event.consume());
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });
    }

    public boolean isDialogOpen() {
        return isVisible();
    }

    public boolean handleEscape() {
        if (!isDialogOpen()) {
            return false;
        }
        close();
        return true;
    }

    public void showCreate(
            String parentLabel,
            boolean rootCategory,
            Consumer<CategoryDraft> onSubmit
    ) {
        TextField nameField = createNameField("");
        TextField iconField = createIconField("◇");
        CheckBox pinned = new CheckBox("Закрепить на главной орбите");
        pinned.getStyleClass().add("category-dialog-check");
        pinned.setSelected(rootCategory);
        pinned.setVisible(rootCategory);
        pinned.setManaged(rootCategory);

        VBox form = new VBox(
                9,
                field("Название", nameField),
                field("Значок или эмодзи", iconField),
                pinned
        );
        showForm(
                "Новая категория",
                "Расположение: " + normalizeParentLabel(parentLabel),
                form,
                "Создать",
                () -> submitCategoryDraft(
                        nameField,
                        iconField,
                        pinned,
                        rootCategory,
                        onSubmit
                )
        );
        nameField.requestFocus();
    }

    public void showEdit(AppCategory category, Consumer<CategoryDraft> onSubmit) {
        TextField nameField = createNameField(category.getName());
        TextField iconField = createIconField(category.getIconKey());
        CheckBox pinned = new CheckBox("Закрепить на главной орбите");
        pinned.getStyleClass().add("category-dialog-check");
        pinned.setSelected(category.isRootPinned());
        pinned.setVisible(category.isRoot());
        pinned.setManaged(category.isRoot());

        VBox form = new VBox(
                9,
                field("Название", nameField),
                field("Значок или эмодзи", iconField),
                pinned
        );
        showForm(
                "Изменить категорию",
                "ID: " + category.getId(),
                form,
                "Сохранить",
                () -> submitCategoryDraft(
                        nameField,
                        iconField,
                        pinned,
                        category.isRoot(),
                        onSubmit
                )
        );
        nameField.requestFocus();
        nameField.selectAll();
    }

    public void showMove(
            AppCategory category,
            List<CategoryMoveTarget> targets,
            Consumer<Long> onSubmit
    ) {
        ComboBox<CategoryMoveTarget> targetBox = new ComboBox<>();
        targetBox.getStyleClass().add("category-dialog-combo");
        targetBox.getItems().setAll(targets == null ? List.of() : targets);
        targetBox.setMaxWidth(Double.MAX_VALUE);

        CategoryMoveTarget current = targetBox.getItems().stream()
                .filter(target -> sameId(
                        target.categoryId(),
                        category.getParentCategoryId()
                ))
                .findFirst()
                .orElse(targetBox.getItems().isEmpty()
                        ? null
                        : targetBox.getItems().get(0));
        targetBox.getSelectionModel().select(current);

        showForm(
                "Переместить «" + category.getName() + "»",
                "Категорию нельзя перемещать внутрь самой себя или своего потомка.",
                field("Новый родитель", targetBox),
                "Переместить",
                () -> {
                    CategoryMoveTarget selected = targetBox.getValue();
                    if (selected == null) {
                        showValidation("Выберите новое расположение.");
                        return;
                    }
                    onSubmit.accept(selected.categoryId());
                }
        );
        targetBox.requestFocus();
    }

    /** Compatibility overload retained for step-6 callers. */
    public void showDelete(AppCategory category, int subtreeSize, Runnable onConfirm) {
        showDelete(category, subtreeSize, 0, onConfirm);
    }

    public void showDelete(
            AppCategory category,
            int categoryCount,
            int shortcutCount,
            Runnable onConfirm
    ) {
        Label warning = new Label(
                "Будет скрыта категория «" + category.getName() + "» и всё её поддерево.\n"
                        + "Категорий: " + Math.max(1, categoryCount) + ".\n"
                        + "Ярлыков: " + Math.max(0, shortcutCount) + ".\n\n"
                        + "Все записи получат общий deletionBatchId. "
                        + "Восстановление через Ctrl+Z будет подключено на этапе 9."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("category-dialog-warning");
        showForm(
                "Удалить категорию?",
                "Операция затронет вложенные категории и ярлыки.",
                warning,
                "Удалить",
                onConfirm,
                true
        );
    }

    public void showCreateShortcut(
            String categoryLabel,
            Consumer<ShortcutDraft> onSubmit
    ) {
        ShortcutForm form = new ShortcutForm(null);
        showForm(
                "Новый ярлык",
                "Категория: " + normalizeParentLabel(categoryLabel),
                form.node(),
                "Создать",
                () -> submitShortcutDraft(form, onSubmit)
        );
        form.nameField().requestFocus();
    }

    public void showEditShortcut(
            AppShortcut shortcut,
            Consumer<ShortcutDraft> onSubmit
    ) {
        ShortcutForm form = new ShortcutForm(shortcut);
        showForm(
                "Изменить ярлык",
                "ID: " + shortcut.getId() + " · " + shortcut.getLaunchType().getDisplayName(),
                form.node(),
                "Сохранить",
                () -> submitShortcutDraft(form, onSubmit)
        );
        form.nameField().requestFocus();
        form.nameField().selectAll();
    }

    public void showMoveShortcut(
            AppShortcut shortcut,
            List<ShortcutMoveTarget> targets,
            Consumer<Long> onSubmit
    ) {
        ComboBox<ShortcutMoveTarget> targetBox = new ComboBox<>();
        targetBox.getStyleClass().add("category-dialog-combo");
        targetBox.getItems().setAll(targets == null ? List.of() : targets);
        targetBox.setMaxWidth(Double.MAX_VALUE);

        ShortcutMoveTarget current = targetBox.getItems().stream()
                .filter(target -> target.categoryId() == shortcut.getCategoryId())
                .findFirst()
                .orElse(targetBox.getItems().isEmpty()
                        ? null
                        : targetBox.getItems().get(0));
        targetBox.getSelectionModel().select(current);

        showForm(
                "Переместить ярлык «" + shortcut.getDisplayName() + "»",
                "Ярлык всегда должен находиться внутри категории.",
                field("Категория назначения", targetBox),
                "Переместить",
                () -> {
                    ShortcutMoveTarget selected = targetBox.getValue();
                    if (selected == null) {
                        showValidation("Выберите категорию назначения.");
                        return;
                    }
                    onSubmit.accept(selected.categoryId());
                }
        );
        targetBox.requestFocus();
    }

    public void showDeleteShortcut(AppShortcut shortcut, Runnable onConfirm) {
        Label warning = new Label(
                "Ярлык «" + shortcut.getDisplayName() + "» будет мягко удалён.\n"
                        + "Цель: " + shortcut.getTarget() + "\n\n"
                        + "Ctrl+Z будет подключён на этапе 9."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("category-dialog-warning");
        showForm(
                "Удалить ярлык?",
                shortcut.getLaunchType().getDisplayName(),
                warning,
                "Удалить",
                onConfirm,
                true
        );
    }

    public void showError(String title, String message) {
        Label body = new Label(message == null ? "Неизвестная ошибка" : message);
        body.setWrapText(true);
        body.getStyleClass().add("category-dialog-error");
        showForm(
                title == null ? "Ошибка" : title,
                null,
                body,
                "Закрыть",
                this::close
        );
    }

    public void showBusy(String message) {
        Label body = new Label(message == null ? "Выполняется операция…" : message);
        body.setWrapText(true);
        body.getStyleClass().add("category-dialog-busy");
        dialogCard.getChildren().setAll(
                title("Пожалуйста, подождите"),
                body
        );
        open();
    }

    public void close() {
        setVisible(false);
        setManaged(false);
        dialogCard.getChildren().clear();
    }

    private void submitCategoryDraft(
            TextField nameField,
            TextField iconField,
            CheckBox pinned,
            boolean supportsPinned,
            Consumer<CategoryDraft> onSubmit
    ) {
        String name = text(nameField);
        String icon = text(iconField);
        if (name.isEmpty()) {
            showValidation("Введите название категории.");
            nameField.requestFocus();
            return;
        }
        if (name.length() > 80) {
            showValidation("Название не должно превышать 80 символов.");
            nameField.requestFocus();
            return;
        }
        if (icon.length() > 16) {
            showValidation("Значок не должен превышать 16 символов.");
            iconField.requestFocus();
            return;
        }
        onSubmit.accept(new CategoryDraft(
                name,
                icon.isEmpty() ? "◇" : icon,
                supportsPinned && pinned.isSelected()
        ));
    }

    private void submitShortcutDraft(
            ShortcutForm form,
            Consumer<ShortcutDraft> onSubmit
    ) {
        String name = text(form.nameField());
        String target = text(form.targetField());
        String arguments = text(form.argumentsField());
        String workingDirectory = text(form.workingDirectoryField());
        String iconSource = text(form.iconSourceField());
        LaunchType launchType = form.typeBox().getValue();

        if (name.isEmpty()) {
            showValidation("Введите название ярлыка.");
            form.nameField().requestFocus();
            return;
        }
        if (name.length() > 120) {
            showValidation("Название ярлыка не должно превышать 120 символов.");
            form.nameField().requestFocus();
            return;
        }
        if (launchType == null) {
            showValidation("Выберите тип цели запуска.");
            form.typeBox().requestFocus();
            return;
        }
        if (target.isEmpty()) {
            showValidation("Укажите цель запуска.");
            form.targetField().requestFocus();
            return;
        }
        if (target.length() > 2000 || arguments.length() > 2000) {
            showValidation("Цель или аргументы слишком длинные.");
            return;
        }
        if (workingDirectory.length() > 1000 || iconSource.length() > 1000) {
            showValidation("Рабочая папка или источник иконки слишком длинные.");
            return;
        }

        onSubmit.accept(new ShortcutDraft(
                name,
                launchType,
                target,
                emptyToNull(arguments),
                emptyToNull(workingDirectory),
                emptyToNull(iconSource),
                form.enabledBox().isSelected()
        ));
    }

    private void showForm(
            String title,
            String subtitle,
            Node body,
            String submitText,
            Runnable onSubmit
    ) {
        showForm(title, subtitle, body, submitText, onSubmit, false);
    }

    private void showForm(
            String title,
            String subtitle,
            Node body,
            String submitText,
            Runnable onSubmit,
            boolean destructive
    ) {
        Label validationLabel = new Label();
        validationLabel.setId("category-dialog-validation");
        validationLabel.getStyleClass().add("category-dialog-validation");
        validationLabel.setWrapText(true);
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);

        Button cancel = button("Отмена", false);
        cancel.setOnAction(event -> close());
        Button submit = button(submitText, destructive);
        submit.setOnAction(event -> onSubmit.run());

        HBox actions = new HBox(10, cancel, submit);
        actions.setAlignment(Pos.CENTER_RIGHT);

        dialogCard.getChildren().clear();
        dialogCard.getChildren().add(title(title));
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setWrapText(true);
            subtitleLabel.getStyleClass().add("category-dialog-subtitle");
            dialogCard.getChildren().add(subtitleLabel);
        }
        dialogCard.getChildren().addAll(body, validationLabel, actions);
        open();
    }

    private Node field(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("category-dialog-field-label");
        VBox box = new VBox(5, label, control);
        box.setFillWidth(true);
        VBox.setVgrow(control, Priority.NEVER);
        return box;
    }

    private Label title(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.getStyleClass().add("category-dialog-title");
        return label;
    }

    private TextField createNameField(String value) {
        TextField field = new TextField(value);
        field.setPromptText("Например: Работа");
        field.getStyleClass().add("category-dialog-input");
        return field;
    }

    private TextField createIconField(String value) {
        TextField field = new TextField(value);
        field.setPromptText("◇");
        field.getStyleClass().add("category-dialog-input");
        return field;
    }

    private Button button(String text, boolean destructive) {
        Button button = new Button(text);
        button.getStyleClass().add("category-dialog-button");
        if (destructive) {
            button.getStyleClass().add("category-dialog-button--danger");
        }
        return button;
    }

    private void showValidation(String message) {
        dialogCard.lookupAll("#category-dialog-validation").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .findFirst()
                .ifPresent(label -> {
                    label.setText(message);
                    label.setVisible(true);
                    label.setManaged(true);
                });
    }

    private void open() {
        setManaged(true);
        setVisible(true);
        toFront();
        requestFocus();
    }

    private String normalizeParentLabel(String value) {
        return value == null || value.isBlank() ? "Корень категорий" : value;
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private final class ShortcutForm {
        private final TextField nameField = new TextField();
        private final ComboBox<LaunchType> typeBox = new ComboBox<>();
        private final TextField targetField = new TextField();
        private final TextField argumentsField = new TextField();
        private final TextField workingDirectoryField = new TextField();
        private final TextField iconSourceField = new TextField();
        private final CheckBox enabledBox = new CheckBox("Ярлык включён");
        private final ScrollPane scrollPane;

        private ShortcutForm(AppShortcut shortcut) {
            nameField.getStyleClass().add("category-dialog-input");
            typeBox.getStyleClass().add("category-dialog-combo");
            targetField.getStyleClass().add("category-dialog-input");
            argumentsField.getStyleClass().add("category-dialog-input");
            workingDirectoryField.getStyleClass().add("category-dialog-input");
            iconSourceField.getStyleClass().add("category-dialog-input");
            enabledBox.getStyleClass().add("category-dialog-check");

            nameField.setPromptText("Например: IntelliJ IDEA");
            targetField.setPromptText("Путь, URL или идентификатор Store-приложения");
            argumentsField.setPromptText("Необязательно; запуск подключится на этапе 8");
            workingDirectoryField.setPromptText("Необязательно");
            iconSourceField.setPromptText("Эмодзи, ключ или путь к иконке");

            typeBox.getItems().setAll(LaunchType.values());
            typeBox.setMaxWidth(Double.MAX_VALUE);
            enabledBox.setSelected(true);

            if (shortcut != null) {
                nameField.setText(shortcut.getDisplayName());
                typeBox.getSelectionModel().select(shortcut.getLaunchType());
                targetField.setText(shortcut.getTarget());
                argumentsField.setText(nullToEmpty(shortcut.getArguments()));
                workingDirectoryField.setText(nullToEmpty(shortcut.getWorkingDirectory()));
                iconSourceField.setText(nullToEmpty(shortcut.getIconSource()));
                enabledBox.setSelected(shortcut.isEnabled());
            } else {
                typeBox.getSelectionModel().select(LaunchType.EXECUTABLE);
            }

            VBox form = new VBox(
                    9,
                    field("Название", nameField),
                    field("Тип", typeBox),
                    field("Цель", targetField),
                    field("Аргументы", argumentsField),
                    field("Рабочая папка", workingDirectoryField),
                    field("Источник иконки", iconSourceField),
                    enabledBox
            );
            form.setFillWidth(true);

            scrollPane = new ScrollPane(form);
            scrollPane.getStyleClass().add("shortcut-dialog-scroll");
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setPrefViewportHeight(430);
            scrollPane.setMaxHeight(470);
        }

        private Node node() {
            return scrollPane;
        }

        private TextField nameField() {
            return nameField;
        }

        private ComboBox<LaunchType> typeBox() {
            return typeBox;
        }

        private TextField targetField() {
            return targetField;
        }

        private TextField argumentsField() {
            return argumentsField;
        }

        private TextField workingDirectoryField() {
            return workingDirectoryField;
        }

        private TextField iconSourceField() {
            return iconSourceField;
        }

        private CheckBox enabledBox() {
            return enabledBox;
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
