package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Maintains one shared visual order for categories and shortcuts that have the
 * same parent category. Root-level siblings contain categories only.
 */
public final class OrbitOrderService {

    public enum RecordKind {
        CATEGORY,
        SHORTCUT
    }

    private static final Comparator<EntryRef> ORDER = Comparator
            .comparingInt(EntryRef::sortOrder)
            .thenComparingInt(ref -> ref.kind() == RecordKind.CATEGORY ? 0 : 1)
            .thenComparingLong(EntryRef::id);

    private final CategoryRepository categoryRepository;
    private final ShortcutRepository shortcutRepository;

    public OrbitOrderService() {
        this(new CategoryRepository(), new ShortcutRepository());
    }

    public OrbitOrderService(
            CategoryRepository categoryRepository,
            ShortcutRepository shortcutRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.shortcutRepository = shortcutRepository;
    }

    public int nextSortOrder(
            List<AppCategory> categories,
            List<AppShortcut> shortcuts,
            Long parentCategoryId
    ) {
        return siblings(categories, shortcuts, parentCategoryId).stream()
                .mapToInt(EntryRef::sortOrder)
                .max()
                .orElse(-1) + 1;
    }

    public boolean moveRelative(
            Connection connection,
            long accountId,
            List<AppCategory> categories,
            List<AppShortcut> shortcuts,
            Long parentCategoryId,
            RecordKind selectedKind,
            long selectedId,
            int direction
    ) throws SQLException {
        if (direction == 0) {
            return false;
        }

        List<EntryRef> siblings = siblings(categories, shortcuts, parentCategoryId);
        int selectedIndex = indexOf(siblings, selectedKind, selectedId);
        int targetIndex = selectedIndex + (direction < 0 ? -1 : 1);
        if (selectedIndex < 0 || targetIndex < 0 || targetIndex >= siblings.size()) {
            return false;
        }

        // First normalize legacy duplicate sort orders, then swap two positions.
        for (int i = 0; i < siblings.size(); i++) {
            updateSortOrder(connection, accountId, siblings.get(i), i);
        }

        EntryRef selected = siblings.get(selectedIndex);
        EntryRef target = siblings.get(targetIndex);
        updateSortOrder(connection, accountId, selected, targetIndex);
        updateSortOrder(connection, accountId, target, selectedIndex);
        return true;
    }

    public void normalize(
            Connection connection,
            long accountId,
            List<AppCategory> categories,
            List<AppShortcut> shortcuts,
            Long parentCategoryId,
            RecordKind excludedKind,
            long excludedId
    ) throws SQLException {
        int order = 0;
        for (EntryRef sibling : siblings(categories, shortcuts, parentCategoryId)) {
            if (sibling.kind() == excludedKind && sibling.id() == excludedId) {
                continue;
            }
            updateSortOrder(connection, accountId, sibling, order++);
        }
    }

    public List<EntryRef> siblings(
            List<AppCategory> categories,
            List<AppShortcut> shortcuts,
            Long parentCategoryId
    ) {
        List<EntryRef> result = new ArrayList<>();

        if (categories != null) {
            for (AppCategory category : categories) {
                if (sameParent(category.getParentCategoryId(), parentCategoryId)) {
                    result.add(new EntryRef(
                            RecordKind.CATEGORY,
                            category.getId(),
                            category.getSortOrder()
                    ));
                }
            }
        }

        // Shortcuts cannot live at the root, only inside a category.
        if (parentCategoryId != null && shortcuts != null) {
            for (AppShortcut shortcut : shortcuts) {
                if (shortcut.getCategoryId() == parentCategoryId) {
                    result.add(new EntryRef(
                            RecordKind.SHORTCUT,
                            shortcut.getId(),
                            shortcut.getSortOrder()
                    ));
                }
            }
        }

        result.sort(ORDER);
        return List.copyOf(result);
    }

    private int indexOf(List<EntryRef> siblings, RecordKind kind, long id) {
        for (int i = 0; i < siblings.size(); i++) {
            EntryRef candidate = siblings.get(i);
            if (candidate.kind() == kind && candidate.id() == id) {
                return i;
            }
        }
        return -1;
    }

    private void updateSortOrder(
            Connection connection,
            long accountId,
            EntryRef entry,
            int sortOrder
    ) throws SQLException {
        if (entry.kind() == RecordKind.CATEGORY) {
            categoryRepository.updateSortOrder(connection, accountId, entry.id(), sortOrder);
        } else {
            shortcutRepository.updateSortOrder(connection, accountId, entry.id(), sortOrder);
        }
    }

    private boolean sameParent(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    public record EntryRef(RecordKind kind, long id, int sortOrder) {
    }
}
