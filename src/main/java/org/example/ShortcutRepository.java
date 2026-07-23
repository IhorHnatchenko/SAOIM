package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** SQL access for application shortcuts. Validation stays in ShortcutService. */
public final class ShortcutRepository {

    public List<AppShortcut> findActiveByAccount(long accountId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findActiveByAccount(connection, accountId, false);
        }
    }

    public List<AppShortcut> findActiveByAccount(
            Connection connection,
            long accountId,
            boolean forUpdate
    ) throws SQLException {
        String sql = "SELECT id, account_id, category_id, display_name, launch_type, target, "
                + "`arguments`, working_directory, icon_source, sort_order, enabled, "
                + "created_at, updated_at "
                + "FROM app_shortcuts "
                + "WHERE account_id = ? AND deleted_at IS NULL "
                + "ORDER BY category_id, sort_order, id"
                + (forUpdate ? " FOR UPDATE" : "");

        List<AppShortcut> shortcuts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    shortcuts.add(map(resultSet));
                }
            }
        }
        return List.copyOf(shortcuts);
    }

    public long insert(
            Connection connection,
            long accountId,
            long categoryId,
            ShortcutDraft draft,
            int sortOrder
    ) throws SQLException {
        String sql = "INSERT INTO app_shortcuts "
                + "(account_id, category_id, display_name, launch_type, target, `arguments`, "
                + "working_directory, icon_source, sort_order, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setLong(1, accountId);
            statement.setLong(2, categoryId);
            bindDraft(statement, draft, 3);
            statement.setInt(9, Math.max(0, sortOrder));
            statement.setBoolean(10, draft.enabled());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Не удалось получить ID созданного ярлыка.");
    }

    public void update(
            Connection connection,
            long accountId,
            long shortcutId,
            ShortcutDraft draft
    ) throws SQLException {
        String sql = "UPDATE app_shortcuts SET display_name = ?, launch_type = ?, target = ?, "
                + "`arguments` = ?, working_directory = ?, icon_source = ?, enabled = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, draft.displayName());
            statement.setString(2, draft.launchType().name());
            statement.setString(3, draft.target());
            setNullable(statement, 4, draft.arguments());
            setNullable(statement, 5, draft.workingDirectory());
            setNullable(statement, 6, draft.iconSource());
            statement.setBoolean(7, draft.enabled());
            statement.setLong(8, shortcutId);
            statement.setLong(9, accountId);
            requireOneRow(statement.executeUpdate(), "Ярлык для изменения не найден.");
        }
    }

    public void updateCategoryAndOrder(
            Connection connection,
            long accountId,
            long shortcutId,
            long categoryId,
            int sortOrder
    ) throws SQLException {
        String sql = "UPDATE app_shortcuts SET category_id = ?, sort_order = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setInt(2, Math.max(0, sortOrder));
            statement.setLong(3, shortcutId);
            statement.setLong(4, accountId);
            requireOneRow(statement.executeUpdate(), "Ярлык для перемещения не найден.");
        }
    }

    public void updateSortOrder(
            Connection connection,
            long accountId,
            long shortcutId,
            int sortOrder
    ) throws SQLException {
        String sql = "UPDATE app_shortcuts SET sort_order = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(0, sortOrder));
            statement.setLong(2, shortcutId);
            statement.setLong(3, accountId);
            requireOneRow(statement.executeUpdate(), "Ярлык для сортировки не найден.");
        }
    }

    public void softDelete(
            Connection connection,
            long accountId,
            long shortcutId,
            String deletionBatchId
    ) throws SQLException {
        String sql = "UPDATE app_shortcuts SET deleted_at = CURRENT_TIMESTAMP, "
                + "deletion_batch_id = ? "
                + "WHERE id = ? AND account_id = ? AND deleted_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, deletionBatchId);
            statement.setLong(2, shortcutId);
            statement.setLong(3, accountId);
            requireOneRow(statement.executeUpdate(), "Ярлык для удаления не найден.");
        }
    }

    public int softDeleteByCategoryIds(
            Connection connection,
            long accountId,
            List<Long> categoryIds,
            String deletionBatchId
    ) throws SQLException {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", Collections.nCopies(categoryIds.size(), "?"));
        String sql = "UPDATE app_shortcuts SET deleted_at = CURRENT_TIMESTAMP, "
                + "deletion_batch_id = ? "
                + "WHERE account_id = ? AND deleted_at IS NULL "
                + "AND category_id IN (" + placeholders + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, deletionBatchId);
            statement.setLong(2, accountId);
            int parameter = 3;
            for (Long categoryId : categoryIds) {
                statement.setLong(parameter++, categoryId);
            }
            return statement.executeUpdate();
        }
    }

    public void softDeleteByIds(
            Connection connection,
            long accountId,
            List<Long> shortcutIds,
            String deletionBatchId
    ) throws SQLException {
        if (shortcutIds == null || shortcutIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(
                ",",
                Collections.nCopies(shortcutIds.size(), "?")
        );
        String sql = "UPDATE app_shortcuts SET deleted_at = CURRENT_TIMESTAMP, "
                + "deletion_batch_id = ? "
                + "WHERE account_id = ? AND deleted_at IS NULL "
                + "AND id IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, deletionBatchId);
            statement.setLong(2, accountId);
            int parameter = 3;
            for (Long shortcutId : shortcutIds) {
                statement.setLong(parameter++, shortcutId);
            }
            int updated = statement.executeUpdate();
            if (updated != shortcutIds.size()) {
                throw new SQLException(
                        "Не все ярлыки удалось пометить удалёнными: "
                                + updated + " из " + shortcutIds.size()
                );
            }
        }
    }

    public void restoreDeleted(
            Connection connection,
            long accountId,
            List<Long> shortcutIds,
            String deletionBatchId
    ) throws SQLException {
        if (shortcutIds == null || shortcutIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(
                ",",
                Collections.nCopies(shortcutIds.size(), "?")
        );
        String sql = "UPDATE app_shortcuts SET deleted_at = NULL, deletion_batch_id = NULL "
                + "WHERE account_id = ? AND deletion_batch_id = ? "
                + "AND deleted_at IS NOT NULL AND id IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, deletionBatchId);
            int parameter = 3;
            for (Long shortcutId : shortcutIds) {
                statement.setLong(parameter++, shortcutId);
            }
            int updated = statement.executeUpdate();
            if (updated != shortcutIds.size()) {
                throw new SQLException(
                        "Не все ярлыки удалось восстановить: "
                                + updated + " из " + shortcutIds.size()
                );
            }
        }
    }

    private void bindDraft(
            PreparedStatement statement,
            ShortcutDraft draft,
            int startIndex
    ) throws SQLException {
        statement.setString(startIndex, draft.displayName());
        statement.setString(startIndex + 1, draft.launchType().name());
        statement.setString(startIndex + 2, draft.target());
        setNullable(statement, startIndex + 3, draft.arguments());
        setNullable(statement, startIndex + 4, draft.workingDirectory());
        setNullable(statement, startIndex + 5, draft.iconSource());
    }

    private void setNullable(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private AppShortcut map(ResultSet resultSet) throws SQLException {
        Timestamp created = resultSet.getTimestamp("created_at");
        Timestamp updated = resultSet.getTimestamp("updated_at");
        return new AppShortcut(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                resultSet.getLong("category_id"),
                resultSet.getString("display_name"),
                parseLaunchType(resultSet.getString("launch_type")),
                resultSet.getString("target"),
                resultSet.getString("arguments"),
                resultSet.getString("working_directory"),
                resultSet.getString("icon_source"),
                resultSet.getInt("sort_order"),
                resultSet.getBoolean("enabled"),
                toLocalDateTime(created),
                toLocalDateTime(updated)
        );
    }

    private LaunchType parseLaunchType(String value) {
        if (value == null || value.isBlank()) {
            return LaunchType.EXECUTABLE;
        }
        try {
            return LaunchType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return LaunchType.EXECUTABLE;
        }
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
