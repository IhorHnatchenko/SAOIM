package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Applies small, idempotent schema upgrades to an existing SAOIM database. */
public final class DatabaseMigrator {
    private DatabaseMigrator() {
    }

    public static void migrate(Connection connection) throws SQLException {
        migrateProfiles(connection);
        migrateCategories(connection);
    }

    private static void migrateProfiles(Connection connection) throws SQLException {
        System.out.println("[DB] Проверка миграций профиля...");
        ensureColumn(connection, "profiles", "nickname", "VARCHAR(50) NULL");
        ensureColumn(
                connection,
                "profiles",
                "title",
                "VARCHAR(50) NOT NULL DEFAULT 'Энтузиаст'"
        );
        ensureColumn(connection, "profiles", "level", "INT NOT NULL DEFAULT 1");
        ensureColumn(connection, "profiles", "current_xp", "INT NOT NULL DEFAULT 0");
        ensureColumn(connection, "profiles", "required_xp", "INT NOT NULL DEFAULT 1000");
        ensureColumn(connection, "profiles", "avatar_uri", "VARCHAR(1000) NULL");
        createMissingProfiles(connection);
        backfillProfileValues(connection);
        ensureUniqueAccountIndex(connection);
        System.out.println("[DB] Миграции профиля применены.");
    }

    private static void migrateCategories(Connection connection) throws SQLException {
        System.out.println("[DB] Проверка миграций категорий...");
        ensureColumn(connection, "app_categories", "parent_category_id", "BIGINT NULL");
        ensureColumn(connection, "app_categories", "icon_key", "VARCHAR(32) NOT NULL DEFAULT '◇'");
        ensureColumn(connection, "app_categories", "root_pinned", "BOOLEAN NOT NULL DEFAULT FALSE");
        ensureColumn(connection, "app_categories", "sort_order", "INT NOT NULL DEFAULT 0");
        ensureColumn(connection, "app_categories", "deletion_batch_id", "CHAR(36) NULL");
        ensureColumn(connection, "app_categories", "deleted_at", "TIMESTAMP NULL DEFAULT NULL");
        ensureColumn(
                connection,
                "app_categories",
                "created_at",
                "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
        );
        ensureColumn(
                connection,
                "app_categories",
                "updated_at",
                "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
        );
        ensureIndex(
                connection,
                "app_categories",
                "ix_categories_account_parent",
                "CREATE INDEX ix_categories_account_parent ON app_categories "
                        + "(account_id, parent_category_id, deleted_at, sort_order)"
        );
        ensureIndex(
                connection,
                "app_categories",
                "ix_categories_deletion_batch",
                "CREATE INDEX ix_categories_deletion_batch ON app_categories "
                        + "(account_id, deletion_batch_id)"
        );
        createDefaultCategoriesForExistingAccounts(connection);
        System.out.println("[DB] Миграции категорий применены.");
    }

    private static void ensureColumn(
            Connection connection,
            String table,
            String column,
            String definition
    ) throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        String sql = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            System.out.println("[DB] Добавлена колонка " + table + "." + column);
        }
    }

    private static boolean columnExists(
            Connection connection,
            String table,
            String column
    ) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void createMissingProfiles(Connection connection) throws SQLException {
        String sql = "INSERT INTO profiles "
                + "(account_id, sao_id, country, city, nickname, title, level, current_xp, required_xp) "
                + "SELECT a.id, "
                + "CONCAT('SAO-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 8))), "
                + "NULL, NULL, a.username, 'Энтузиаст', 1, 0, 1000 "
                + "FROM accounts a "
                + "LEFT JOIN profiles p ON p.account_id = a.id "
                + "WHERE p.account_id IS NULL";
        try (Statement statement = connection.createStatement()) {
            int inserted = statement.executeUpdate(sql);
            if (inserted > 0) {
                System.out.println("[DB] Созданы отсутствующие профили: " + inserted);
            }
        }
    }

    private static void backfillProfileValues(Connection connection) throws SQLException {
        String nicknameSql = "UPDATE profiles p "
                + "JOIN accounts a ON a.id = p.account_id "
                + "SET p.nickname = a.username "
                + "WHERE p.nickname IS NULL OR TRIM(p.nickname) = ''";
        String defaultsSql = "UPDATE profiles SET "
                + "title = COALESCE(NULLIF(TRIM(title), ''), 'Энтузиаст'), "
                + "level = CASE WHEN level IS NULL OR level < 1 THEN 1 ELSE level END, "
                + "current_xp = CASE WHEN current_xp IS NULL OR current_xp < 0 THEN 0 ELSE current_xp END, "
                + "required_xp = CASE WHEN required_xp IS NULL OR required_xp < 1 "
                + "THEN 1000 ELSE required_xp END";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(nicknameSql);
            statement.executeUpdate(defaultsSql);
        }
    }

    private static void ensureUniqueAccountIndex(Connection connection) throws SQLException {
        if (indexExists(connection, "profiles", "uk_profiles_account_id")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE profiles ADD UNIQUE INDEX uk_profiles_account_id (account_id)"
            );
            System.out.println("[DB] Добавлен уникальный индекс profiles.account_id");
        } catch (SQLException exception) {
            System.err.println(
                    "[DB] Не удалось добавить уникальный индекс profiles.account_id: "
                            + exception.getMessage()
            );
        }
    }

    private static void ensureIndex(
            Connection connection,
            String table,
            String indexName,
            String createSql
    ) throws SQLException {
        if (indexExists(connection, table, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
            System.out.println("[DB] Добавлен индекс " + table + "." + indexName);
        }
    }

    private static boolean indexExists(
            Connection connection,
            String table,
            String indexName
    ) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void createDefaultCategoriesForExistingAccounts(
            Connection connection
    ) throws SQLException {
        List<Long> accountIds = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id FROM accounts")) {
            while (resultSet.next()) {
                accountIds.add(resultSet.getLong(1));
            }
        }
        CategoryRepository repository = new CategoryRepository();
        for (Long accountId : accountIds) {
            repository.createDefaultCategories(connection, accountId);
        }
    }
}
