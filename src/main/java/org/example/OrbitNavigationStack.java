package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stores the current path inside the orbit tree without changing MenuState.
 */
public final class OrbitNavigationStack {

    private final List<OrbitEntry> rootEntries;
    private final Deque<OrbitEntry> path = new ArrayDeque<>();

    public OrbitNavigationStack(List<OrbitEntry> rootEntries) {
        this.rootEntries = rootEntries == null ? List.of() : List.copyOf(rootEntries);
    }

    public List<OrbitEntry> getCurrentEntries() {
        OrbitEntry current = path.peekLast();
        return current == null ? rootEntries : current.getChildren();
    }

    public boolean enter(OrbitEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (!entry.isCategory() || !entry.hasChildren()) {
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
                .collect(Collectors.joining("  ›  "));
    }

    public List<OrbitEntry> getPathSnapshot() {
        return List.copyOf(new ArrayList<>(path));
    }

    public void reset() {
        path.clear();
    }
}
