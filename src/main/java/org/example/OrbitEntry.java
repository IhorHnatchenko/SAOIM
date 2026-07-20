package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable data displayed by CircularMenuPane.
 *
 * Step 4 uses in-memory demo entries. The same UI contract can later be filled
 * by AppCategory/AppShortcut data loaded from repositories.
 */
public final class OrbitEntry {

    public enum Kind {
        CATEGORY,
        ITEM
    }

    private final String id;
    private final String title;
    private final String icon;
    private final String description;
    private final Kind kind;
    private final List<OrbitEntry> children;

    public OrbitEntry(
            String id,
            String title,
            String icon,
            String description,
            Kind kind,
            List<OrbitEntry> children
    ) {
        this.id = normalize(id, "entry");
        this.title = normalize(title, "Без названия");
        this.icon = normalize(icon, "◇");
        this.description = normalize(description, "Нет описания");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    public static OrbitEntry category(
            String id,
            String title,
            String icon,
            String description,
            OrbitEntry... children
    ) {
        return new OrbitEntry(
                id,
                title,
                icon,
                description,
                Kind.CATEGORY,
                children == null ? List.of() : Arrays.asList(children)
        );
    }

    public static OrbitEntry item(
            String id,
            String title,
            String icon,
            String description
    ) {
        return new OrbitEntry(id, title, icon, description, Kind.ITEM, List.of());
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Kind getKind() {
        return kind;
    }

    public List<OrbitEntry> getChildren() {
        return children;
    }

    public boolean isCategory() {
        return kind == Kind.CATEGORY;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
