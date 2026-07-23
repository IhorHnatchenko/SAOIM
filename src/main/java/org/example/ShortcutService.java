package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/** Transactional CRUD service for user shortcuts. */
public final class ShortcutService {
    private final CategoryRepository categoryRepository;
    private final ShortcutRepository shortcutRepository;
    private final CategoryTreeService treeService;
    private final OrbitOrderService orderService;

    public ShortcutService() {
        this(
                new CategoryRepository(),
                new ShortcutRepository(),
                new CategoryTreeService(),
                new OrbitOrderService()
        );
    }

    public ShortcutService(
            CategoryRepository categoryRepository,
            ShortcutRepository shortcutRepository,
            CategoryTreeService treeService,
            OrbitOrderService orderService
    ) {
        this.categoryRepository = categoryRepository;
        this.shortcutRepository = shortcutRepository;
        this.treeService = treeService;
        this.orderService = orderService;
    }

    public long createShortcut(
            long accountId,
            long categoryId,
            ShortcutDraft draft
    ) throws SQLException {
        validateAccount(accountId);
        ShortcutDraft validated = validateDraft(draft);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = categoryRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                requireCategory(categories, accountId, categoryId);
                int sortOrder = orderService.nextSortOrder(
                        categories,
                        shortcuts,
                        categoryId
                );
                long id = shortcutRepository.insert(
                        connection,
                        accountId,
                        categoryId,
                        validated,
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

    public void updateShortcut(
            long accountId,
            long shortcutId,
            ShortcutDraft draft
    ) throws SQLException {
        validateAccount(accountId);
        ShortcutDraft validated = validateDraft(draft);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                requireShortcut(shortcuts, accountId, shortcutId);
                shortcutRepository.update(
                        connection,
                        accountId,
                        shortcutId,
                        validated
                );
                connection.commit();
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public void moveShortcut(
            long accountId,
            long shortcutId,
            long newCategoryId
    ) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = categoryRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppShortcut shortcut = requireShortcut(shortcuts, accountId, shortcutId);
                requireCategory(categories, accountId, newCategoryId);
                if (shortcut.getCategoryId() == newCategoryId) {
                    connection.commit();
                    return;
                }

                int sortOrder = orderService.nextSortOrder(
                        categories,
                        shortcuts,
                        newCategoryId
                );
                shortcutRepository.updateCategoryAndOrder(
                        connection,
                        accountId,
                        shortcutId,
                        newCategoryId,
                        sortOrder
                );
                orderService.normalize(
                        connection,
                        accountId,
                        categories,
                        shortcuts,
                        shortcut.getCategoryId(),
                        OrbitOrderService.RecordKind.SHORTCUT,
                        shortcutId
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
            long shortcutId,
            int direction
    ) throws SQLException {
        validateAccount(accountId);
        if (direction == 0) {
            return false;
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = categoryRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppShortcut shortcut = requireShortcut(shortcuts, accountId, shortcutId);
                boolean changed = orderService.moveRelative(
                        connection,
                        accountId,
                        categories,
                        shortcuts,
                        shortcut.getCategoryId(),
                        OrbitOrderService.RecordKind.SHORTCUT,
                        shortcutId,
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

    public String softDeleteShortcut(long accountId, long shortcutId) throws SQLException {
        validateAccount(accountId);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<AppCategory> categories = categoryRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                List<AppShortcut> shortcuts = shortcutRepository.findActiveByAccount(
                        connection,
                        accountId,
                        true
                );
                AppShortcut shortcut = requireShortcut(shortcuts, accountId, shortcutId);
                String batchId = UUID.randomUUID().toString();
                shortcutRepository.softDelete(
                        connection,
                        accountId,
                        shortcutId,
                        batchId
                );
                orderService.normalize(
                        connection,
                        accountId,
                        categories,
                        shortcuts,
                        shortcut.getCategoryId(),
                        OrbitOrderService.RecordKind.SHORTCUT,
                        shortcutId
                );
                connection.commit();
                return batchId;
            } catch (Exception exception) {
                rollbackQuietly(connection);
                throw rethrow(exception);
            }
        }
    }

    public List<ShortcutMoveTarget> getMoveTargets(
            CategorySnapshot snapshot,
            long shortcutId
    ) {
        if (snapshot == null || snapshot.findShortcut(shortcutId) == null) {
            return List.of();
        }
        return treeService.buildShortcutMoveTargets(snapshot.getCategories());
    }

    private ShortcutDraft validateDraft(ShortcutDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Данные ярлыка отсутствуют.");
        }
        String name = normalize(draft.displayName());
        String target = normalize(draft.target());
        String arguments = normalizeNullable(draft.arguments());
        String workingDirectory = normalizeNullable(draft.workingDirectory());
        String iconSource = normalizeNullable(draft.iconSource());
        LaunchType launchType = draft.launchType() == null
                ? LaunchType.EXECUTABLE
                : draft.launchType();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Введите название ярлыка.");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException(
                    "Название ярлыка не должно превышать 120 символов."
            );
        }
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Укажите цель запуска.");
        }
        if (target.length() > 2000) {
            throw new IllegalArgumentException("Цель запуска слишком длинная.");
        }
        if (arguments != null && arguments.length() > 2000) {
            throw new IllegalArgumentException("Строка аргументов слишком длинная.");
        }
        if (workingDirectory != null && workingDirectory.length() > 1000) {
            throw new IllegalArgumentException("Рабочая папка слишком длинная.");
        }
        if (iconSource != null && iconSource.length() > 1000) {
            throw new IllegalArgumentException("Источник иконки слишком длинный.");
        }

        return new ShortcutDraft(
                name,
                launchType,
                target,
                arguments,
                workingDirectory,
                iconSource,
                draft.enabled()
        );
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
                        "Категория назначения не найдена в текущем аккаунте."
                ));
    }

    private AppShortcut requireShortcut(
            List<AppShortcut> shortcuts,
            long accountId,
            long shortcutId
    ) {
        return shortcuts.stream()
                .filter(shortcut -> shortcut.getId() == shortcutId)
                .filter(shortcut -> shortcut.getAccountId() == accountId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ярлык не найден в текущем аккаунте."
                ));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
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
