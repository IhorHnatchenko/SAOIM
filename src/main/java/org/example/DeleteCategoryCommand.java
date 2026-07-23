package org.example;

/** Reversible cascade soft-delete of one category subtree. */
public final class DeleteCategoryCommand implements UndoableCommand {
    private final CategoryService categoryService;
    private final long accountId;
    private final long categoryId;
    private final String categoryName;

    private DeletionBatch deletionBatch;
    private State state = State.NEW;

    public DeleteCategoryCommand(
            CategoryService categoryService,
            long accountId,
            long categoryId,
            String categoryName
    ) {
        this.categoryService = categoryService == null
                ? new CategoryService()
                : categoryService;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.categoryName = normalizeName(categoryName, "категория");
    }

    @Override
    public void execute() throws Exception {
        requireState(State.NEW, "Команда удаления категории уже выполнялась.");
        deletionBatch = categoryService.softDeleteSubtreeForUndo(accountId, categoryId);
        state = State.DELETED;
    }

    @Override
    public void undo() throws Exception {
        requireState(State.DELETED, "Категория уже восстановлена.");
        categoryService.restoreDeletion(deletionBatch);
        state = State.RESTORED;
    }

    @Override
    public void redo() throws Exception {
        requireState(State.RESTORED, "Повторное удаление сейчас недоступно.");
        categoryService.redoDeletion(deletionBatch);
        state = State.DELETED;
    }

    @Override
    public String description() {
        return "удаление категории «" + categoryName + "»";
    }

    public DeletionBatch getDeletionBatch() {
        return deletionBatch;
    }

    private void requireState(State expected, String message) {
        if (state != expected) {
            throw new IllegalStateException(message);
        }
    }

    private String normalizeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private enum State {
        NEW,
        DELETED,
        RESTORED
    }
}
