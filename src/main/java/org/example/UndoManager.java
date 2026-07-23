package org.example;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Session-scoped history of reversible operations.
 *
 * The manager changes stack state only after the corresponding database
 * operation succeeds. A failed undo/redo therefore remains available for a
 * retry and never corrupts the history.
 */
public final class UndoManager {
    private static final int DEFAULT_LIMIT = 50;

    private final Deque<UndoableCommand> undoStack = new ArrayDeque<>();
    private final Deque<UndoableCommand> redoStack = new ArrayDeque<>();
    private final int limit;

    public UndoManager() {
        this(DEFAULT_LIMIT);
    }

    public UndoManager(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Лимит истории должен быть положительным.");
        }
        this.limit = limit;
    }

    public void execute(UndoableCommand command) throws Exception {
        if (command == null) {
            throw new IllegalArgumentException("Команда отсутствует.");
        }
        command.execute();
        synchronized (this) {
            undoStack.push(command);
            redoStack.clear();
            trimUndoHistory();
        }
    }

    public Optional<String> undo() throws Exception {
        UndoableCommand command;
        synchronized (this) {
            command = undoStack.peek();
        }
        if (command == null) {
            return Optional.empty();
        }

        command.undo();
        synchronized (this) {
            if (undoStack.peek() != command) {
                throw new IllegalStateException(
                        "История изменилась во время выполнения отмены."
                );
            }
            undoStack.pop();
            redoStack.push(command);
        }
        return Optional.of(command.description());
    }

    public Optional<String> redo() throws Exception {
        UndoableCommand command;
        synchronized (this) {
            command = redoStack.peek();
        }
        if (command == null) {
            return Optional.empty();
        }

        command.redo();
        synchronized (this) {
            if (redoStack.peek() != command) {
                throw new IllegalStateException(
                        "История изменилась во время выполнения повтора."
                );
            }
            redoStack.pop();
            undoStack.push(command);
            trimUndoHistory();
        }
        return Optional.of(command.description());
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized Optional<String> nextUndoDescription() {
        return undoStack.isEmpty()
                ? Optional.empty()
                : Optional.of(undoStack.peek().description());
    }

    public synchronized Optional<String> nextRedoDescription() {
        return redoStack.isEmpty()
                ? Optional.empty()
                : Optional.of(redoStack.peek().description());
    }

    public synchronized int undoCount() {
        return undoStack.size();
    }

    public synchronized int redoCount() {
        return redoStack.size();
    }

    public synchronized void discardRedo() {
        redoStack.clear();
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private void trimUndoHistory() {
        while (undoStack.size() > limit) {
            undoStack.removeLast();
        }
    }
}
