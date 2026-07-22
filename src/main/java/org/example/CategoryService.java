package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Transactional business service for the category tree. */
public final class CategoryService {
    private final CategoryRepository repository;
    private final CategoryTreeService treeService;
    private final PinningService pinningService;

    public CategoryService() {
        this(new CategoryRepository(), new CategoryTreeService(), new PinningService());
    }

    public CategoryService(
            CategoryRepository repository,
            CategoryTreeService treeService,
            PinningService pinningService
    ) {
        this.repository = repository;
        this.treeService = treeService;
        this.pinningService = pinningService;
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
                connection.commit();
                return treeService.buildSnapshot(accountId, categories);
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
                AppCategory parent = parentCategoryId == null
                        ? null
                        : requireCategory(categories, accountId, parentCategoryId);
                if (parent != null && parent.getAccountId() != accountId) {
                    throw new IllegalArgumentException("Родитель принадлежит другому аккаунту.");
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
                int sortOrder = repository.getNextSortOrder(
                        connection,
                        accountId,
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
                    pinningService.validateToggle(categories, category, validated.rootPinned());
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
                int sortOrder = repository.getNextSortOrder(
                        connection,
                        accountId,
                        newParentCategoryId
                );
                repository.updateParentAndOrder(
                        connection,
                        accountId,
                        categoryId,
                        newParentCategoryId,
                        sortOrder
                );
                normalizeSiblings(connection, accountId, categories, category.getParentCategoryId(), categoryId);
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
                AppCategory category = requireCategory(categories, accountId, categoryId);
                List<AppCategory> siblings = treeService.siblingsOf(categories, category);
                int index = indexOf(siblings, categoryId);
                int targetIndex = index + (direction < 0 ? -1 : 1);
                if (index < 0 || targetIndex < 0 || targetIndex >= siblings.size()) {
                    connection.commit();
                    return false;
                }
                // Normalize first so duplicate/legacy sort_order values cannot break a swap.
                for (int i = 0; i < siblings.size(); i++) {
                    repository.updateSortOrder(
                            connection,
                            accountId,
                            siblings.get(i).getId(),
                            i
                    );
                }
                AppCategory target = siblings.get(targetIndex);
                repository.updateSortOrder(connection, accountId, categoryId, targetIndex);
                repository.updateSortOrder(connection, accountId, target.getId(), index);
                connection.commit();
                return true;
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
                repository.setRootPinned(connection, accountId, categoryId, requested);
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

    /**
     * Step 6 already uses soft deletion so step 9 can later attach Ctrl+Z to the
     * same deletion_batch_id without changing the storage contract.
     */
    public String softDeleteSubtree(long accountId, long categoryId) throws SQLException {
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
                Set<Long> ids = new LinkedHashSet<>();
                ids.add(category.getId());
                ids.addAll(treeService.collectDescendantIds(categories, categoryId));
                String batchId = UUID.randomUUID().toString();
                repository.softDelete(
                        connection,
                        accountId,
                        new ArrayList<>(ids),
                        batchId
                );
                normalizeSiblings(
                        connection,
                        accountId,
                        categories,
                        category.getParentCategoryId(),
                        categoryId
                );
                connection.commit();
                return batchId;
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

    private void normalizeSiblings(
            Connection connection,
            long accountId,
            List<AppCategory> categories,
            Long parentCategoryId,
            long excludedCategoryId
    ) throws SQLException {
        int order = 0;
        for (AppCategory category : categories.stream()
                .filter(candidate -> candidate.getId() != excludedCategoryId)
                .filter(candidate -> sameParent(
                        candidate.getParentCategoryId(),
                        parentCategoryId
                ))
                .sorted(java.util.Comparator
                        .comparingInt(AppCategory::getSortOrder)
                        .thenComparingLong(AppCategory::getId))
                .toList()) {
            repository.updateSortOrder(connection, accountId, category.getId(), order++);
        }
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
            throw new IllegalArgumentException("Название категории не должно превышать 80 символов.");
        }
        if (icon.isEmpty()) {
            icon = "◇";
        }
        if (icon.length() > 16) {
            throw new IllegalArgumentException("Значок не должен превышать 16 символов.");
        }
        return new CategoryDraft(name, icon, draft.rootPinned());
    }

    private int indexOf(List<AppCategory> categories, long categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == categoryId) {
                return i;
            }
        }
        return -1;
    }

    private boolean sameParent(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
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
