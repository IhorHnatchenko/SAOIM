package org.example;

/** Reversible soft-delete of one application shortcut. */
public final class DeleteShortcutCommand implements UndoableCommand {
    private final ShortcutService shortcutService;
    private final long accountId;
    private final long shortcutId;
    private final String shortcutName;

    private DeletionBatch deletionBatch;
    private State state = State.NEW;

    public DeleteShortcutCommand(
            ShortcutService shortcutService,
            long accountId,
            long shortcutId,
            String shortcutName
    ) {
        this.shortcutService = shortcutService == null
                ? new ShortcutService()
                : shortcutService;
        this.accountId = accountId;
        this.shortcutId = shortcutId;
        this.shortcutName = normalizeName(shortcutName, "ярлык");
    }

    @Override
    public void execute() throws Exception {
        requireState(State.NEW, "Команда удаления ярлыка уже выполнялась.");
        deletionBatch = shortcutService.softDeleteShortcutForUndo(accountId, shortcutId);
        state = State.DELETED;
    }

    @Override
    public void undo() throws Exception {
        requireState(State.DELETED, "Ярлык уже восстановлен.");
        shortcutService.restoreDeletion(deletionBatch);
        state = State.RESTORED;
    }

    @Override
    public void redo() throws Exception {
        requireState(State.RESTORED, "Повторное удаление сейчас недоступно.");
        shortcutService.redoDeletion(deletionBatch);
        state = State.DELETED;
    }

    @Override
    public String description() {
        return "удаление ярлыка «" + shortcutName + "»";
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
