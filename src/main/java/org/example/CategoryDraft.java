package org.example;

/** Values entered in the category create/edit overlay. */
public record CategoryDraft(String name, String iconKey, boolean rootPinned) {
    public CategoryDraft {
        name = name == null ? "" : name.trim();
        iconKey = iconKey == null || iconKey.isBlank() ? "◇" : iconKey.trim();
    }
}
