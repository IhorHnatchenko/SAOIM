package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL access for user categories. Business validation lives in CategoryService. */
public final class CategoryRepository {
    public static final String DEFAULT_WORK_NAME = "Работа";
    public static final String DEFAULT_MEDIA_NAME = "Медиа";
    public static final String DEFAULT_SYSTEM_NAME = "Система";

    public List<AppCategory> findActiveByAccount(long accountId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findActiveByAccount(connection, accountId, false);
        }
    }

    public List<AppCategory> findActiveByAccount(
            Connection connection,
            long accountId,
            boolean forUpdate
    ) throws SQLException {
        String sql = "SELECT id, account_id, parent_category_id, name, icon_key, "
                + "root_pinned, sort_order, created_at, updated_at "
                + "FROM app_categories "
                + "WHERE account_id = ? AND deleted_at IS NULL "
                + "ORDER BY parent_category_id IS NOT NULL, parent_category_id, sort_order, id"
                + (forUpdate ? " FOR UPDATE" : "");

        List<AppCategory> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(map(resultSet));
                }
            }
        }
        return categories;
    }

    public long insert(
            Connection connection,
            long accountId,
            Long parentCategoryId,
            String name,
            String iconKey,
            boolean rootPinned,
            int sortOrder
    ) throws SQLException {
        String sql = "INSERT INTO app_categories "
                + "(account_id, parent_category_id, name, icon_key, root_pinned, sort_order) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setLong(1, accountId);
            if (parentCategoryId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, parentCategoryId);
            }
            statement.setString(3, name);
            statement.setString(4, iconKey);
            statement.setBoolean(5, parentCategoryId == null && rootPinned);
            statement.setInt(6, Math.max(0, sortOrder));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось получить ID созданной категории.");
    }

    public void updateNameAndIcon(
            Connection connection,
            long accountId,
            long categoryId,
            String name,
            String iconKey
    ) throws SQLException {
        String sql = "UPDATE app_categories SET name = ?, icon_key = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, iconKey);
            statement.setLong(3, categoryId);
            statement.setLong(4, accountId);
            requireOneRow(statement.executeUpdate(), "Категория для переименования не найдена.");
        }
    }

    public void updateParentAndOrder(
            Connection connection,
            long accountId,
            long categoryId,
            Long parentCategoryId,
            int sortOrder
    ) throws SQLException {
        String sql = "UPDATE app_categories "
                + "SET parent_category_id = ?, sort_order = ?, "
                + "root_pinned = CASE WHEN ? IS NULL THEN root_pinned ELSE FALSE END "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parentCategoryId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, parentCategoryId);
                statement.setLong(3, parentCategoryId);
            }
            statement.setInt(2, Math.max(0, sortOrder));
            statement.setLong(4, categoryId);
            statement.setLong(5, accountId);
            requireOneRow(statement.executeUpdate(), "Категория для перемещения не найдена.");
        }
    }

    public void updateSortOrder(
            Connection connection,
            long accountId,
            long categoryId,
            int sortOrder
    ) throws SQLException {
        String sql = "UPDATE app_categories SET sort_order = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(0, sortOrder));
            statement.setLong(2, categoryId);
            statement.setLong(3, accountId);
            requireOneRow(statement.executeUpdate(), "Категория для сортировки не найдена.");
        }
    }

    public void setRootPinned(
            Connection connection,
            long accountId,
            long categoryId,
            boolean pinned
    ) throws SQLException {
        String sql = "UPDATE app_categories SET root_pinned = ? "
                + "WHERE id = ? AND account_id = ? "
                + "AND parent_category_id IS NULL AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, pinned);
            statement.setLong(2, categoryId);
            statement.setLong(3, accountId);
            requireOneRow(statement.executeUpdate(), "Закреплять можно только корневую категорию.");
        }
    }

    public void softDelete(
            Connection connection,
            long accountId,
            List<Long> categoryIds,
            String deletionBatchId
    ) throws SQLException {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(categoryIds.size(), "?"));
        String sql = "UPDATE app_categories SET deleted_at = CURRENT_TIMESTAMP, "
                + "deletion_batch_id = ?, root_pinned = FALSE "
                + "WHERE account_id = ? AND deleted_at IS NULL AND id IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, deletionBatchId);
            statement.setLong(2, accountId);
            int parameter = 3;
            for (Long id : categoryIds) {
                statement.setLong(parameter++, id);
            }
            statement.executeUpdate();
        }
    }

    public int getNextSortOrder(
            Connection connection,
            long accountId,
            Long parentCategoryId
    ) throws SQLException {
        String sql = "SELECT COALESCE(MAX(sort_order), -1) + 1 "
                + "FROM app_categories WHERE account_id = ? AND deleted_at IS NULL "
                + "AND parent_category_id <=> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            if (parentCategoryId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, parentCategoryId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Math.max(0, resultSet.getInt(1)) : 0;
            }
        }
    }

    public void createDefaultCategories(Connection connection, long accountId) throws SQLException {
        if (hasAnyCategory(connection, accountId)) {
            return;
        }
        insert(connection, accountId, null, DEFAULT_WORK_NAME, "▣", true, 0);
        insert(connection, accountId, null, DEFAULT_MEDIA_NAME, "▶", true, 1);
        insert(connection, accountId, null, DEFAULT_SYSTEM_NAME, "⚙", true, 2);
        System.out.println("[Categories] Созданы стартовые категории для accountId=" + accountId);
    }

    public boolean hasAnyCategory(Connection connection, long accountId) throws SQLException {
        String sql = "SELECT 1 FROM app_categories "
                + "WHERE account_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private AppCategory map(ResultSet resultSet) throws SQLException {
        long parentValue = resultSet.getLong("parent_category_id");
        Long parentId = resultSet.wasNull() ? null : parentValue;
        Timestamp created = resultSet.getTimestamp("created_at");
        Timestamp updated = resultSet.getTimestamp("updated_at");
        return new AppCategory(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                parentId,
                resultSet.getString("name"),
                resultSet.getString("icon_key"),
                resultSet.getBoolean("root_pinned"),
                resultSet.getInt("sort_order"),
                toLocalDateTime(created),
                toLocalDateTime(updated)
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void requireOneRow(int count, String message) throws SQLException {
        if (count != 1) {
            throw new SQLException(message);
        }
    }
}
