package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Stores the current path inside the orbit tree without changing MenuState. */
public final class OrbitNavigationStack {
    private List<OrbitEntry> rootEntries;
    private final Deque<OrbitEntry> path = new ArrayDeque<>();

    public OrbitNavigationStack(List<OrbitEntry> rootEntries) {
        this.rootEntries = safeCopy(rootEntries);
    }

    public List<OrbitEntry> getCurrentEntries() {
        OrbitEntry current = path.peekLast();
        return current == null ? rootEntries : current.getChildren();
    }

    public OrbitEntry getCurrentCategory() {
        return path.peekLast();
    }

    public Long getCurrentCategoryId() {
        OrbitEntry current = getCurrentCategory();
        return current == null ? null : current.getCategoryId();
    }

    public boolean enter(OrbitEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (!entry.isCategory()) {
            return false;
        }
        path.addLast(entry);
        return true;
    }

    public boolean goBack() {
        if (path.isEmpty()) {
            return false;
        }
        path.removeLast();
        return true;
    }

    public boolean canGoBack() {
        return !path.isEmpty();
    }

    public boolean isAtRoot() {
        return path.isEmpty();
    }

    public int getDepth() {
        return path.size();
    }

    public String getCurrentTitle() {
        OrbitEntry current = path.peekLast();
        return current == null ? "Центр управления" : current.getTitle();
    }

    public String getBreadcrumb() {
        if (path.isEmpty()) {
            return "Корень категорий";
        }
        return path.stream()
                .map(OrbitEntry::getTitle)
                .collect(Collectors.joining(" › "));
    }

    public List<OrbitEntry> getPathSnapshot() {
        return List.copyOf(new ArrayList<>(path));
    }

    public List<Long> getPathCategoryIds() {
        return path.stream()
                .map(OrbitEntry::getCategoryId)
                .filter(Objects::nonNull)
                .toList();
    }

    public void replaceRootEntries(List<OrbitEntry> newRootEntries, List<Long> preferredPathIds) {
        rootEntries = safeCopy(newRootEntries);
        path.clear();
        restorePath(preferredPathIds);
    }

    public void restorePath(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<OrbitEntry> level = rootEntries;
        for (Long id : categoryIds) {
            if (id == null) {
                break;
            }
            OrbitEntry match = level.stream()
                    .filter(OrbitEntry::isCategory)
                    .filter(entry -> id.equals(entry.getCategoryId()))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                break;
            }
            path.addLast(match);
            level = match.getChildren();
        }
    }

    public void reset() {
        path.clear();
    }

    private static List<OrbitEntry> safeCopy(List<OrbitEntry> entries) {
        return entries == null ? List.of() : List.copyOf(entries);
    }
}
