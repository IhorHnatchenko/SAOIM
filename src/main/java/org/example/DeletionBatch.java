package org.example;

import java.util.List;
import java.util.Objects;

/**
 * Exact set of rows affected by one reversible soft-delete operation.
 * The row identifiers are kept in memory so redo does not depend on a
 * persistent undo_operations table.
 */
public record DeletionBatch(
        String batchId,
        long accountId,
        List<Long> categoryIds,
        List<Long> shortcutIds
) {
    public DeletionBatch {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("deletionBatchId отсутствует.");
        }
        if (accountId <= 0) {
            throw new IllegalArgumentException("Некорректный accountId удаления.");
        }
        categoryIds = immutableIds(categoryIds);
        shortcutIds = immutableIds(shortcutIds);
        if (categoryIds.isEmpty() && shortcutIds.isEmpty()) {
            throw new IllegalArgumentException("Пакет удаления не содержит записей.");
        }
    }

    public boolean containsCategories() {
        return !categoryIds.isEmpty();
    }

    public boolean containsShortcuts() {
        return !shortcutIds.isEmpty();
    }

    private static List<Long> immutableIds(List<Long> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
