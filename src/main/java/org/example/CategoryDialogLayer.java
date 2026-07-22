package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Modal in-scene dialogs that stay on the monitor owned by the overlay. */
public final class CategoryDialogLayer extends StackPane {
    private final StackPane dimmer = new StackPane();
    private final VBox dialogCard = new VBox(12);

    public CategoryDialogLayer() {
        getStyleClass().add("category-dialog-layer");
        dimmer.getStyleClass().add("category-dialog-dimmer");
        dialogCard.getStyleClass().add("category-dialog-card");
        dialogCard.setAlignment(Pos.CENTER_LEFT);
        dialogCard.setPadding(new Insets(22));
        dialogCard.setMaxWidth(440);
        dialogCard.setMinWidth(360);

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
                () -> submitDraft(nameField, iconField, pinned, rootCategory, onSubmit)
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
                () -> submitDraft(
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
                .filter(target -> sameId(target.categoryId(), category.getParentCategoryId()))
                .findFirst()
                .orElse(targetBox.getItems().isEmpty() ? null : targetBox.getItems().get(0));
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

    public void showDelete(
            AppCategory category,
            int subtreeSize,
            Runnable onConfirm
    ) {
        Label warning = new Label(
                "Будет скрыта категория «" + category.getName() + "» и всё её поддерево.\n"
                        + "Количество категорий: " + Math.max(1, subtreeSize) + ".\n\n"
                        + "Данные помечаются как удалённые. Восстановление через Ctrl+Z "
                        + "будет подключено на этапе 9."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("category-dialog-warning");
        showForm(
                "Удалить категорию?",
                "Операция затронет вложенные категории.",
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

    private void submitDraft(
            TextField nameField,
            TextField iconField,
            CheckBox pinned,
            boolean supportsPinned,
            Consumer<CategoryDraft> onSubmit
    ) {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String icon = iconField.getText() == null ? "" : iconField.getText().trim();
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

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }
}
