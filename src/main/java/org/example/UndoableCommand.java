package org.example;

/** A reversible business operation executed outside the JavaFX thread. */
public interface UndoableCommand {
    void execute() throws Exception;

    void undo() throws Exception;

    void redo() throws Exception;

    String description();
}
