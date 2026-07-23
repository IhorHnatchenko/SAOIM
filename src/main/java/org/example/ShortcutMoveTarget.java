package org.example;

/** One valid destination category shown by the shortcut move dialog. */
public record ShortcutMoveTarget(long categoryId, String label) {
    public ShortcutMoveTarget {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Shortcut destination category must be positive");
        }
        label = label == null || label.isBlank() ? "Категория " + categoryId : label.trim();
    }

    @Override
    public String toString() {
        return label;
    }
}
