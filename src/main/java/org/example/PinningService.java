package org.example;

import java.util.List;

/** Rules for categories shown on the main orbit. */
public final class PinningService {
    /** The user changed the visible orbit page from five to eight in step 4. */
    public static final int MAX_PINNED_ROOT_CATEGORIES = 8;

    public void validateToggle(
            List<AppCategory> categories,
            AppCategory category,
            boolean requestedPinned
    ) {
        if (category == null || !category.isRoot()) {
            throw new IllegalArgumentException("Закреплять можно только корневые категории.");
        }
        if (!requestedPinned || category.isRootPinned()) {
            return;
        }
        long pinnedCount = categories.stream()
                .filter(AppCategory::isRoot)
                .filter(AppCategory::isRootPinned)
                .count();
        if (pinnedCount >= MAX_PINNED_ROOT_CATEGORIES) {
            throw new IllegalStateException(
                    "На главной орбите уже закреплено "
                            + MAX_PINNED_ROOT_CATEGORIES
                            + " категорий. Сначала открепите одну из них."
            );
        }
    }
}
