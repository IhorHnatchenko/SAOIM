package org.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoManagerTest {
    @Test
    void executesUndoesAndRedoesCommand() throws Exception {
        List<String> events = new ArrayList<>();
        UndoManager manager = new UndoManager();
        RecordingCommand command = new RecordingCommand("Удаление теста", events);

        manager.execute(command);
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());
        assertEquals(List.of("execute"), events);

        assertEquals("Удаление теста", manager.undo().orElseThrow());
        assertEquals(List.of("execute", "undo"), events);
        assertFalse(manager.canUndo());
        assertTrue(manager.canRedo());

        assertEquals("Удаление теста", manager.redo().orElseThrow());
        assertEquals(List.of("execute", "undo", "redo"), events);
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());
    }

    @Test
    void failedUndoRemainsAvailableForRetry() throws Exception {
        UndoManager manager = new UndoManager();
        FailingUndoCommand command = new FailingUndoCommand();
        manager.execute(command);

        assertThrows(IllegalStateException.class, manager::undo);

        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());
        assertEquals(1, manager.undoCount());
    }

    @Test
    void newExecutionClearsRedoStack() throws Exception {
        UndoManager manager = new UndoManager();
        manager.execute(new RecordingCommand("first", new ArrayList<>()));
        manager.undo();
        assertTrue(manager.canRedo());

        manager.execute(new RecordingCommand("second", new ArrayList<>()));

        assertFalse(manager.canRedo());
        assertEquals("second", manager.nextUndoDescription().orElseThrow());
    }

    @Test
    void respectsConfiguredHistoryLimit() throws Exception {
        UndoManager manager = new UndoManager(2);
        manager.execute(new RecordingCommand("one", new ArrayList<>()));
        manager.execute(new RecordingCommand("two", new ArrayList<>()));
        manager.execute(new RecordingCommand("three", new ArrayList<>()));

        assertEquals(2, manager.undoCount());
        assertEquals("three", manager.undo().orElseThrow());
        assertEquals("two", manager.undo().orElseThrow());
        assertTrue(manager.undo().isEmpty());
    }

    private static final class RecordingCommand implements UndoableCommand {
        private final String description;
        private final List<String> events;

        private RecordingCommand(String description, List<String> events) {
            this.description = description;
            this.events = events;
        }

        @Override
        public void execute() {
            events.add("execute");
        }

        @Override
        public void undo() {
            events.add("undo");
        }

        @Override
        public void redo() {
            events.add("redo");
        }

        @Override
        public String description() {
            return description;
        }
    }

    private static final class FailingUndoCommand implements UndoableCommand {
        @Override
        public void execute() {
        }

        @Override
        public void undo() {
            throw new IllegalStateException("database unavailable");
        }

        @Override
        public void redo() {
        }

        @Override
        public String description() {
            return "failing";
        }
    }
}
