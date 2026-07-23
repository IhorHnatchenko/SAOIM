package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable data displayed by CircularMenuPane. */
public final class OrbitEntry {
    public enum Kind {
        CATEGORY,
        SHORTCUT,
        ITEM
    }

    private final String id;
    private final Long categoryId;
    private final Long shortcutId;
    private final String title;
    private final String icon;
    private final String description;
    private final Kind kind;
    private final boolean rootPinned;
    private final int sortOrder;
    private final boolean enabled;
    private final LaunchType launchType;
    private final List<OrbitEntry> children;

    public OrbitEntry(
            String id,
            Long categoryId,
            String title,
            String icon,
            String description,
            Kind kind,
            boolean rootPinned,
            int sortOrder,
            List<OrbitEntry> children
    ) {
        this(
                id,
                categoryId,
                null,
                title,
                icon,
                description,
                kind,
                rootPinned,
                sortOrder,
                true,
                null,
                children
        );
    }

    public OrbitEntry(
            String id,
            Long categoryId,
            Long shortcutId,
            String title,
            String icon,
            String description,
            Kind kind,
            boolean rootPinned,
            int sortOrder,
            boolean enabled,
            LaunchType launchType,
            List<OrbitEntry> children
    ) {
        this.id = normalize(id, "entry");
        this.categoryId = categoryId;
        this.shortcutId = shortcutId;
        this.title = normalize(title, "Без названия");
        this.icon = normalize(icon, "◇");
        this.description = normalize(description, "Нет описания");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.rootPinned = rootPinned;
        this.sortOrder = Math.max(0, sortOrder);
        this.enabled = enabled;
        this.launchType = launchType;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    /** Compatibility constructor used by older step-4 code. */
    public OrbitEntry(
            String id,
            String title,
            String icon,
            String description,
            Kind kind,
            List<OrbitEntry> children
    ) {
        this(id, null, null, title, icon, description, kind, false, 0, true, null, children);
    }

    public static OrbitEntry category(
            long categoryId,
            String title,
            String icon,
            String description,
            boolean rootPinned,
            int sortOrder,
            List<OrbitEntry> children
    ) {
        return new OrbitEntry(
                "category:" + categoryId,
                categoryId,
                null,
                title,
                icon,
                description,
                Kind.CATEGORY,
                rootPinned,
                sortOrder,
                true,
                null,
                children
        );
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
                null,
                null,
                title,
                icon,
                description,
                Kind.CATEGORY,
                false,
                0,
                true,
                null,
                children == null ? List.of() : Arrays.asList(children)
        );
    }

    public static OrbitEntry shortcut(AppShortcut shortcut) {
        Objects.requireNonNull(shortcut, "shortcut");
        String description = shortcut.getLaunchType().getDisplayName()
                + "\nЦель: " + shortcut.getTarget()
                + (shortcut.isEnabled() ? "" : "\nЯрлык отключён");
        return new OrbitEntry(
                "shortcut:" + shortcut.getId(),
                null,
                shortcut.getId(),
                shortcut.getDisplayName(),
                shortcut.getDisplayIcon(),
                description,
                Kind.SHORTCUT,
                false,
                shortcut.getSortOrder(),
                shortcut.isEnabled(),
                shortcut.getLaunchType(),
                List.of()
        );
    }

    public static OrbitEntry item(
            String id,
            String title,
            String icon,
            String description
    ) {
        return new OrbitEntry(
                id,
                null,
                null,
                title,
                icon,
                description,
                Kind.ITEM,
                false,
                0,
                true,
                null,
                List.of()
        );
    }

    public String getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getShortcutId() {
        return shortcutId;
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

    public boolean isRootPinned() {
        return rootPinned;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LaunchType getLaunchType() {
        return launchType;
    }

    public List<OrbitEntry> getChildren() {
        return children;
    }

    public boolean isCategory() {
        return kind == Kind.CATEGORY;
    }

    public boolean isShortcut() {
        return kind == Kind.SHORTCUT;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean isDatabaseCategory() {
        return isCategory() && categoryId != null && categoryId > 0;
    }

    public boolean isDatabaseShortcut() {
        return isShortcut() && shortcutId != null && shortcutId > 0;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
