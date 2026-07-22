package org.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable active category state loaded for one account. */
public final class CategorySnapshot {
    private final long accountId;
    private final List<AppCategory> categories;
    private final Map<Long, AppCategory> byId;
    private final List<OrbitEntry> pinnedRootEntries;
    private final List<OrbitEntry> allRootEntries;

    public CategorySnapshot(
            long accountId,
            List<AppCategory> categories,
            List<OrbitEntry> pinnedRootEntries,
            List<OrbitEntry> allRootEntries
    ) {
        this.accountId = accountId;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
        Map<Long, AppCategory> index = new LinkedHashMap<>();
        for (AppCategory category : this.categories) {
            index.put(category.getId(), category);
        }
        this.byId = Map.copyOf(index);
        this.pinnedRootEntries = pinnedRootEntries == null ? List.of() : List.copyOf(pinnedRootEntries);
        this.allRootEntries = allRootEntries == null ? List.of() : List.copyOf(allRootEntries);
    }

    public long getAccountId() {
        return accountId;
    }

    public List<AppCategory> getCategories() {
        return categories;
    }

    public Map<Long, AppCategory> getById() {
        return byId;
    }

    public AppCategory find(long categoryId) {
        return byId.get(categoryId);
    }

    public List<OrbitEntry> getPinnedRootEntries() {
        return pinnedRootEntries;
    }

    public List<OrbitEntry> getAllRootEntries() {
        return allRootEntries;
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }
}
