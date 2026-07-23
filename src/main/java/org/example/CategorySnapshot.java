package org.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable active category and shortcut state loaded for one account. */
public final class CategorySnapshot {
    private final long accountId;
    private final List<AppCategory> categories;
    private final List<AppShortcut> shortcuts;
    private final Map<Long, AppCategory> categoriesById;
    private final Map<Long, AppShortcut> shortcutsById;
    private final List<OrbitEntry> pinnedRootEntries;
    private final List<OrbitEntry> allRootEntries;

    /** Compatibility constructor for stage-6 callers. */
    public CategorySnapshot(
            long accountId,
            List<AppCategory> categories,
            List<OrbitEntry> pinnedRootEntries,
            List<OrbitEntry> allRootEntries
    ) {
        this(accountId, categories, List.of(), pinnedRootEntries, allRootEntries);
    }

    public CategorySnapshot(
            long accountId,
            List<AppCategory> categories,
            List<AppShortcut> shortcuts,
            List<OrbitEntry> pinnedRootEntries,
            List<OrbitEntry> allRootEntries
    ) {
        this.accountId = accountId;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
        this.shortcuts = shortcuts == null ? List.of() : List.copyOf(shortcuts);

        Map<Long, AppCategory> categoryIndex = new LinkedHashMap<>();
        for (AppCategory category : this.categories) {
            categoryIndex.put(category.getId(), category);
        }
        categoriesById = Map.copyOf(categoryIndex);

        Map<Long, AppShortcut> shortcutIndex = new LinkedHashMap<>();
        for (AppShortcut shortcut : this.shortcuts) {
            shortcutIndex.put(shortcut.getId(), shortcut);
        }
        shortcutsById = Map.copyOf(shortcutIndex);

        this.pinnedRootEntries = pinnedRootEntries == null
                ? List.of()
                : List.copyOf(pinnedRootEntries);
        this.allRootEntries = allRootEntries == null
                ? List.of()
                : List.copyOf(allRootEntries);
    }

    public long getAccountId() {
        return accountId;
    }

    public List<AppCategory> getCategories() {
        return categories;
    }

    public List<AppShortcut> getShortcuts() {
        return shortcuts;
    }

    public Map<Long, AppCategory> getById() {
        return categoriesById;
    }

    public Map<Long, AppShortcut> getShortcutsById() {
        return shortcutsById;
    }

    public AppCategory find(long categoryId) {
        return categoriesById.get(categoryId);
    }

    public AppShortcut findShortcut(long shortcutId) {
        return shortcutsById.get(shortcutId);
    }

    public List<OrbitEntry> getPinnedRootEntries() {
        return pinnedRootEntries;
    }

    public List<OrbitEntry> getAllRootEntries() {
        return allRootEntries;
    }

    public boolean isEmpty() {
        return categories.isEmpty() && shortcuts.isEmpty();
    }
}
