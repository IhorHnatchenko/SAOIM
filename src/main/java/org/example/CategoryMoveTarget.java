package org.example;

/** One valid destination shown in the move-category dialog. */
public record CategoryMoveTarget(Long categoryId, String label) {
    public CategoryMoveTarget {
        label = label == null || label.isBlank() ? "Корень категорий" : label.trim();
    }

    @Override
    public String toString() {
        return label;
    }
}
