package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Transactional business service for the category tree and mixed orbit order. */
public final class CategoryService {
    private final CategoryRepository repository;
    private final ShortcutRepository shortcutRepository;
    private final CategoryTreeService treeService;
    private final PinningService pinningService;
    private final OrbitOrderService orderService;

    public CategoryService() {
        this(
                new CategoryRepository(),
                new ShortcutRepository(),
                new CategoryTreeService(),
                new PinningService(),
                new OrbitOrderService()
        );
    }

    /** Compatibility constructor used by stage-6 tests/callers. */
    public CategoryService(
            CategoryRepository repository,
            CategoryTreeService treeService,
            PinningService pinningService
    ) {
        this(
                repository,
                new ShortcutRepository(),
                treeService,
                pinningService,
                new OrbitOrderService(repository, new ShortcutRepository())
        );
    }

    public CategoryService(
            CategoryRepository repository,
            ShortcutRepository shortcutRepository,
            CategoryTreeService treeService,
            PinningService pinningService,
            OrbitOrderService orderService
    ) {
        this.repository = repository;
        this.shortcutRepository = shortcutRepository;
        this.treeService = treeService;
        this.pinningService = pinningService;
        this.orderService = orderService;
    }

    public CategorySnapshot loadSnapshot(long accountId) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                repository.createDefaultCategories(connection, accountId);
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        false
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        false
                );
                connection.commit();
                return treeService.buildSnapshot(accountId, categories, shortcuts);
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public long createCategory(
            long accountId,
            Long parentCategoryId,
            CategoryDraft draft
    ) throws SQLException {
        validateAccount(accountId);
        CategoryDraft validated = validateDraft(draft);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory parent = parentCategoryId == null
                        ? null
                        : requireCategory(categories, accountId, parentCategoryId);
                if (parent != null && parent.getAccountId() != accountId) {
                    throw new IllegalArgumentException(
                            "Родитель принадлежит другому аккаунту."
                    );
                }

                boolean pin = parentCategoryId == null && validated.rootPinned();
                if (pin) {
                    AppCategory virtualCategory = new AppCategory(
                            Long.MAX_VALUE,
                            accountId,
                            null,
                            validated.name(),
                            validated.iconKey(),
                            false,
                            0,
                            null,
                            null
                    );
                    pinningService.validateToggle(categories, virtualCategory, true);
                }

                int sortOrder = orderService.nextSortOrder(
                        categories,
                        shortcuts,
                        parentCategoryId
                );
                long id = repository.insert(
                        connection,
                        accountId,
                        parentCategoryId,
                        validated.name(),
                        validated.iconKey(),
                        pin,
                        sortOrder
                );
                connection.commit();
                return id;
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public void updateCategory(
            long accountId,
            long categoryId,
            CategoryDraft draft
    ) throws SQLException {
        validateAccount(accountId);
        CategoryDraft validated = validateDraft(draft);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory category = requireCategory(categories, accountId, categoryId);
                repository.updateNameAndIcon(
                        connection,
                        accountId,
                        categoryId,
                        validated.name(),
                        validated.iconKey()
                );
                if (category.isRoot()) {
                    pinningService.validateToggle(
                            categories,
                            category,
                            validated.rootPinned()
                    );
                    repository.setRootPinned(
                            connection,
                            accountId,
                            categoryId,
                            validated.rootPinned()
                    );
                }
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public void moveCategory(
            long accountId,
            long categoryId,
            Long newParentCategoryId
    ) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory category = requireCategory(categories, accountId, categoryId);
                treeService.validateMove(
                        accountId,
                        categories,
                        categoryId,
                        newParentCategoryId
                );
                if (sameParent(category.getParentCategoryId(), newParentCategoryId)) {
                    connection.commit();
                    return;
                }

                int sortOrder = orderService.nextSortOrder(
                        categories,
                        shortcuts,
                        newParentCategoryId
                );
                repository.updateParentAndOrder(
                        connection,
                        accountId,
                        categoryId,
                        newParentCategoryId,
                        sortOrder
                );
                orderService.normalize(
                        connection,
                        accountId,
                        categories,
                        shortcuts,
                        category.getParentCategoryId(),
                        OrbitOrderService.RecordKind.CATEGORY,
                        categoryId
                );
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public boolean moveRelative(
            long accountId,
            long categoryId,
            int direction
    ) throws SQLException {
        validateAccount(accountId);
        if (direction == 0) {
            return false;
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory category = requireCategory(categories, accountId, categoryId);
                boolean changed = orderService.moveRelative(
                        connection,
                        accountId,
                        categories,
                        shortcuts,
                        category.getParentCategoryId(),
                        OrbitOrderService.RecordKind.CATEGORY,
                        categoryId,
                        direction
                );
                connection.commit();
                return changed;
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public void togglePinned(long accountId, long categoryId) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory category = requireCategory(categories, accountId, categoryId);
                boolean requested = !category.isRootPinned();
                pinningService.validateToggle(categories, category, requested);
                repository.setRootPinned(
                        connection,
                        accountId,
                        categoryId,
                        requested
                );
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public int countSubtree(long accountId, long categoryId) throws SQLException {
        return countSubtree(loadSnapshot(accountId), categoryId);
    }

    public int countSubtree(CategorySnapshot snapshot, long categoryId) {
        if (snapshot == null) {
            return 0;
        }
        requireCategory(snapshot.getCategories(), snapshot.getAccountId(), categoryId);
        return 1 + treeService.collectDescendantIds(
                snapshot.getCategories(),
                categoryId
        ).size();
    }

    public int countShortcutsInSubtree(CategorySnapshot snapshot, long categoryId) {
        if (snapshot == null) {
            return 0;
        }
        Set<Long> categoryIds = new LinkedHashSet<>();
        categoryIds.add(categoryId);
        categoryIds.addAll(treeService.collectDescendantIds(
                snapshot.getCategories(),
                categoryId
        ));
        int count = 0;
        for (AppShortcut shortcut : snapshot.getShortcuts()) {
            if (categoryIds.contains(shortcut.getCategoryId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compatibility method retained for existing callers.
     * New code should use {@link #softDeleteSubtreeForUndo(long, long)}.
     */
    public String softDeleteSubtree(long accountId, long categoryId) throws SQLException {
        return softDeleteSubtreeForUndo(accountId, categoryId).batchId();
    }

    /**
     * Soft-deletes the complete subtree and returns the exact row set required
     * for lossless undo/redo. Sort orders and rootPinned values are deliberately
     * left untouched, so restoration returns every item to its previous place.
     */
    public DeletionBatch softDeleteSubtreeForUndo(
            long accountId,
            long categoryId
    ) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = repository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppCategory category = requireCategory(categories, accountId, categoryId);
                Set<Long> categoryIdSet = new LinkedHashSet<>();
                categoryIdSet.add(category.getId());
                categoryIdSet.addAll(treeService.collectDescendantIds(categories, categoryId));

                List<Long> categoryIds = new ArrayList<>(categoryIdSet);
                List<Long> shortcutIds = shortcuts.stream()
                        .filter(shortcut -> categoryIdSet.contains(shortcut.getCategoryId()))
                        .map(AppShortcut::getId)
                        .toList();

                String batchId = UUID.randomUUID().toString();
                shortcutRepository.softDeleteByIds(
                        connection,
                        accountId,
                        shortcutIds,
                        batchId
                );
                repository.softDelete(
                        connection,
                        accountId,
                        categoryIds,
                        batchId
                );
                connection.commit();
                return new DeletionBatch(
                        batchId,
                        accountId,
                        categoryIds,
                        shortcutIds
                );
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    /** Restores a previously deleted category subtree in one transaction. */
    public void restoreDeletion(DeletionBatch batch) throws SQLException {
        validateBatch(batch, true);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int activePinned = repository.countActivePinnedRoots(
                        connection,
                        batch.accountId()
                );
                int restoringPinned = repository.countDeletedPinnedRoots(
                        connection,
                        batch.accountId(),
                        batch.batchId()
                );
                if (activePinned + restoringPinned
                        > PinningService.MAX_PINNED_ROOT_CATEGORIES) {
                    throw new IllegalStateException(
                            "Восстановление превысит лимит закреплённых категорий. "
                                    + "Сначала открепите одну из активных категорий."
                    );
                }

                repository.restoreDeleted(
                        connection,
                        batch.accountId(),
                        batch.categoryIds(),
                        batch.batchId()
                );
                shortcutRepository.restoreDeleted(
                        connection,
                        batch.accountId(),
                        batch.shortcutIds(),
                        batch.batchId()
                );
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    /** Re-applies a category deletion after it has been undone. */
    public void redoDeletion(DeletionBatch batch) throws SQLException {
        validateBatch(batch, true);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                shortcutRepository.softDeleteByIds(
                        connection,
                        batch.accountId(),
                        batch.shortcutIds(),
                        batch.batchId()
                );
                repository.softDelete(
                        connection,
                        batch.accountId(),
                        batch.categoryIds(),
                        batch.batchId()
                );
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public List<CategoryMoveTarget> getMoveTargets(
            CategorySnapshot snapshot,
            long categoryId
    ) {
        if (snapshot == null) {
            return List.of();
        }
        return treeService.buildMoveTargets(snapshot.getCategories(), categoryId);
    }

    private AppCategory requireCategory(
            List<AppCategory> categories,
            long accountId,
            long categoryId
    ) {
        return categories.stream()
                .filter(category -> category.getId() == categoryId)
                .filter(category -> category.getAccountId() == accountId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Категория не найдена в текущем аккаунте."
                ));
    }

    private CategoryDraft validateDraft(CategoryDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Данные категории отсутствуют.");
        }
        String name = draft.name() == null ? "" : draft.name().trim();
        String icon = draft.iconKey() == null ? "◇" : draft.iconKey().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Введите название категории.");
        }
        if (name.length() > 80) {
            throw new IllegalArgumentException(
                    "Название категории не должно превышать 80 символов."
            );
        }
        if (icon.isEmpty()) {
            icon = "◇";
        }
        if (icon.length() > 16) {
            throw new IllegalArgumentException(
                    "Значок не должен превышать 16 символов."
            );
        }
        return new CategoryDraft(name, icon, draft.rootPinned());
    }

    private boolean sameParent(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private void validateBatch(DeletionBatch batch, boolean requireCategories) {
        if (batch == null) {
            throw new IllegalArgumentException("Пакет удаления отсутствует.");
        }
        validateAccount(batch.accountId());
        if (requireCategories && batch.categoryIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "Пакет не содержит категорий для восстановления."
            );
        }
    }

    private void validateAccount(long accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Требуется авторизованный аккаунт.");
        }
    }

    private SQLException rethrow(Exception exception) throws SQLException {
        if (exception instanceof SQLException sqlException) {
            return sqlException;
        }
        return new SQLException(exception.getMessage(), exception);
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            rollbackException.printStackTrace();
        }
    }
}
