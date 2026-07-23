package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the category tree, injects shortcuts and protects moves from cycles. */
public final class CategoryTreeService {
    private static final Comparator<AppCategory> CATEGORY_ORDER = Comparator
            .comparingInt(AppCategory::getSortOrder)
            .thenComparingLong(AppCategory::getId);

    private static final Comparator<OrbitEntry> ORBIT_ORDER = Comparator
            .comparingInt(OrbitEntry::getSortOrder)
            .thenComparingInt(entry -> entry.isCategory() ? 0 : 1)
            .thenComparing(OrbitEntry::getId);

    /** Compatibility overload for code that only loads categories. */
    public CategorySnapshot buildSnapshot(long accountId, List<AppCategory> categories) {
        return buildSnapshot(accountId, categories, List.of());
    }

    public CategorySnapshot buildSnapshot(
            long accountId,
            List<AppCategory> categories,
            List<AppShortcut> shortcuts
    ) {
        List<AppCategory> safeCategories = categories == null ? List.of() : List.copyOf(categories);
        List<AppShortcut> safeShortcuts = shortcuts == null ? List.of() : List.copyOf(shortcuts);

        validateTree(accountId, safeCategories);
        validateShortcuts(accountId, safeCategories, safeShortcuts);

        Map<Long, List<AppCategory>> childrenByParent = groupChildren(safeCategories);
        Map<Long, List<AppShortcut>> shortcutsByCategory = groupShortcuts(safeShortcuts);

        List<AppCategory> roots = safeCategories.stream()
                .filter(AppCategory::isRoot)
                .sorted(CATEGORY_ORDER)
                .toList();

        List<OrbitEntry> allRoots = roots.stream()
                .map(category -> toOrbitEntry(
                        category,
                        childrenByParent,
                        shortcutsByCategory,
                        new HashSet<>()
                ))
                .toList();

        List<OrbitEntry> pinnedRoots = roots.stream()
                .filter(AppCategory::isRootPinned)
                .map(category -> toOrbitEntry(
                        category,
                        childrenByParent,
                        shortcutsByCategory,
                        new HashSet<>()
                ))
                .toList();

        return new CategorySnapshot(
                accountId,
                safeCategories,
                safeShortcuts,
                pinnedRoots,
                allRoots
        );
    }

    public void validateMove(
            long accountId,
            List<AppCategory> categories,
            long categoryId,
            Long newParentId
    ) {
        Map<Long, AppCategory> byId = index(categories);
        AppCategory category = requireOwned(byId, accountId, categoryId);
        if (newParentId == null) {
            return;
        }
        requireOwned(byId, accountId, newParentId);
        if (categoryId == newParentId) {
            throw new IllegalArgumentException("Нельзя переместить категорию внутрь самой себя.");
        }
        Set<Long> descendants = collectDescendantIds(categories, categoryId);
        if (descendants.contains(newParentId)) {
            throw new IllegalArgumentException("Нельзя переместить категорию внутрь её потомка.");
        }
        if (category.getParentCategoryId() != null
                && category.getParentCategoryId().equals(newParentId)) {
            return;
        }
    }

    public Set<Long> collectDescendantIds(List<AppCategory> categories, long categoryId) {
        Map<Long, List<AppCategory>> childrenByParent = groupChildren(categories);
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            long current = queue.removeFirst();
            for (AppCategory child : childrenByParent.getOrDefault(current, List.of())) {
                if (result.add(child.getId())) {
                    queue.addLast(child.getId());
                }
            }
        }
        return result;
    }

    public List<CategoryMoveTarget> buildMoveTargets(
            List<AppCategory> categories,
            long movingCategoryId
    ) {
        Map<Long, AppCategory> byId = index(categories);
        if (!byId.containsKey(movingCategoryId)) {
            return List.of();
        }
        Set<Long> forbidden = collectDescendantIds(categories, movingCategoryId);
        forbidden.add(movingCategoryId);
        Map<Long, String> paths = buildPaths(categories);

        List<CategoryMoveTarget> targets = new ArrayList<>();
        targets.add(new CategoryMoveTarget(null, "Корень категорий"));
        categories.stream()
                .filter(category -> !forbidden.contains(category.getId()))
                .sorted(Comparator.comparing(
                        category -> paths.getOrDefault(category.getId(), category.getName()),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .forEach(category -> targets.add(new CategoryMoveTarget(
                        category.getId(),
                        paths.getOrDefault(category.getId(), category.getName())
                )));
        return List.copyOf(targets);
    }

    public List<ShortcutMoveTarget> buildShortcutMoveTargets(List<AppCategory> categories) {
        Map<Long, String> paths = buildPaths(categories);
        return categories.stream()
                .sorted(Comparator.comparing(
                        category -> paths.getOrDefault(category.getId(), category.getName()),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(category -> new ShortcutMoveTarget(
                        category.getId(),
                        paths.getOrDefault(category.getId(), category.getName())
                ))
                .toList();
    }

    public List<AppCategory> siblingsOf(
            List<AppCategory> categories,
            AppCategory category
    ) {
        return categories.stream()
                .filter(candidate -> sameParent(
                        candidate.getParentCategoryId(),
                        category.getParentCategoryId()
                ))
                .sorted(CATEGORY_ORDER)
                .toList();
    }

    private OrbitEntry toOrbitEntry(
            AppCategory category,
            Map<Long, List<AppCategory>> childrenByParent,
            Map<Long, List<AppShortcut>> shortcutsByCategory,
            Set<Long> recursionGuard
    ) {
        if (!recursionGuard.add(category.getId())) {
            throw new IllegalStateException(
                    "Обнаружен цикл категорий возле id=" + category.getId()
            );
        }

        List<AppCategory> directCategories = childrenByParent
                .getOrDefault(category.getId(), List.of())
                .stream()
                .sorted(CATEGORY_ORDER)
                .toList();
        List<AppShortcut> directShortcuts = shortcutsByCategory
                .getOrDefault(category.getId(), List.of());

        List<OrbitEntry> children = new ArrayList<>();
        for (AppCategory child : directCategories) {
            children.add(toOrbitEntry(
                    child,
                    childrenByParent,
                    shortcutsByCategory,
                    new HashSet<>(recursionGuard)
            ));
        }
        for (AppShortcut shortcut : directShortcuts) {
            children.add(OrbitEntry.shortcut(shortcut));
        }
        children.sort(ORBIT_ORDER);

        String description;
        if (children.isEmpty()) {
            description = "Пустая категория — добавьте подкатегорию или ярлык";
        } else {
            description = "Подкатегорий: " + directCategories.size()
                    + " · ярлыков: " + directShortcuts.size();
        }

        return OrbitEntry.category(
                category.getId(),
                category.getName(),
                category.getIconKey(),
                description,
                category.isRootPinned(),
                category.getSortOrder(),
                children
        );
    }

    private void validateTree(long accountId, List<AppCategory> categories) {
        Map<Long, AppCategory> byId = index(categories);
        for (AppCategory category : categories) {
            if (category.getAccountId() != accountId) {
                throw new IllegalStateException(
                        "В дереве обнаружена категория другого аккаунта."
                );
            }
            Long parentId = category.getParentCategoryId();
            if (parentId != null && !byId.containsKey(parentId)) {
                throw new IllegalStateException(
                        "У категории id=" + category.getId()
                                + " отсутствует активный родитель."
                );
            }
        }

        Map<Long, Integer> colors = new HashMap<>();
        for (AppCategory category : categories) {
            detectCycle(category.getId(), byId, colors);
        }
    }

    private void validateShortcuts(
            long accountId,
            List<AppCategory> categories,
            List<AppShortcut> shortcuts
    ) {
        Map<Long, AppCategory> categoriesById = index(categories);
        for (AppShortcut shortcut : shortcuts) {
            if (shortcut.getAccountId() != accountId) {
                throw new IllegalStateException(
                        "В снимке обнаружен ярлык другого аккаунта."
                );
            }
            if (!categoriesById.containsKey(shortcut.getCategoryId())) {
                throw new IllegalStateException(
                        "У ярлыка id=" + shortcut.getId()
                                + " отсутствует активная категория."
                );
            }
        }
    }

    private void detectCycle(
            long categoryId,
            Map<Long, AppCategory> byId,
            Map<Long, Integer> colors
    ) {
        int color = colors.getOrDefault(categoryId, 0);
        if (color == 1) {
            throw new IllegalStateException("Обнаружен цикл в дереве категорий.");
        }
        if (color == 2) {
            return;
        }
        colors.put(categoryId, 1);
        AppCategory category = byId.get(categoryId);
        if (category != null && category.getParentCategoryId() != null) {
            detectCycle(category.getParentCategoryId(), byId, colors);
        }
        colors.put(categoryId, 2);
    }

    private Map<Long, List<AppCategory>> groupChildren(List<AppCategory> categories) {
        Map<Long, List<AppCategory>> result = new LinkedHashMap<>();
        if (categories == null) {
            return result;
        }
        for (AppCategory category : categories) {
            Long parentId = category.getParentCategoryId();
            if (parentId != null) {
                result.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
            }
        }
        result.values().forEach(list -> list.sort(CATEGORY_ORDER));
        return result;
    }

    private Map<Long, List<AppShortcut>> groupShortcuts(List<AppShortcut> shortcuts) {
        Map<Long, List<AppShortcut>> result = new LinkedHashMap<>();
        if (shortcuts == null) {
            return result;
        }
        for (AppShortcut shortcut : shortcuts) {
            result.computeIfAbsent(shortcut.getCategoryId(), ignored -> new ArrayList<>())
                    .add(shortcut);
        }
        result.values().forEach(list -> list.sort(
                Comparator.comparingInt(AppShortcut::getSortOrder)
                        .thenComparingLong(AppShortcut::getId)
        ));
        return result;
    }

    private Map<Long, AppCategory> index(List<AppCategory> categories) {
        Map<Long, AppCategory> byId = new LinkedHashMap<>();
        if (categories != null) {
            for (AppCategory category : categories) {
                byId.put(category.getId(), category);
            }
        }
        return byId;
    }

    private AppCategory requireOwned(
            Map<Long, AppCategory> byId,
            long accountId,
            long categoryId
    ) {
        AppCategory category = byId.get(categoryId);
        if (category == null || category.getAccountId() != accountId) {
            throw new IllegalArgumentException(
                    "Категория не найдена в текущем аккаунте."
            );
        }
        return category;
    }

    private Map<Long, String> buildPaths(List<AppCategory> categories) {
        Map<Long, AppCategory> byId = index(categories);
        Map<Long, String> paths = new HashMap<>();
        for (AppCategory category : categories) {
            paths.put(category.getId(), buildPath(category, byId));
        }
        return paths;
    }

    private String buildPath(
            AppCategory category,
            Map<Long, AppCategory> byId
    ) {
        Deque<String> segments = new ArrayDeque<>();
        Set<Long> guard = new HashSet<>();
        AppCategory current = category;
        while (current != null && guard.add(current.getId())) {
            segments.addFirst(current.getName());
            Long parentId = current.getParentCategoryId();
            current = parentId == null ? null : byId.get(parentId);
        }
        return String.join(" › ", segments);
    }

    private boolean sameParent(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }
}
