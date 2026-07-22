package org.example;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private static final String URL = DOTENV.get(
            "DB_URL",
            "jdbc:mysql://127.0.0.1:3307/sao_server?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    );
    private static final String USER = DOTENV.get("DB_USER", "root");
    private static final String PASSWORD = DOTENV.get("DB_PASSWORD", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            System.err.println("[DB] Драйвер MySQL не найден: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initDatabase() {
        System.out.println("[DB] Инициализация соединения с базой данных...");

        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(32) NOT NULL UNIQUE, "
                + "email VARCHAR(255) NOT NULL UNIQUE, "
                + "password_hash VARCHAR(255) NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createProfilesTable = "CREATE TABLE IF NOT EXISTS profiles ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "account_id INT NOT NULL, "
                + "sao_id VARCHAR(50) NOT NULL UNIQUE, "
                + "country VARCHAR(100), "
                + "city VARCHAR(100), "
                + "nickname VARCHAR(50), "
                + "title VARCHAR(50) NOT NULL DEFAULT 'Энтузиаст', "
                + "level INT NOT NULL DEFAULT 1, "
                + "current_xp INT NOT NULL DEFAULT 0, "
                + "required_xp INT NOT NULL DEFAULT 1000, "
                + "avatar_uri VARCHAR(1000), "
                + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createCategoriesTable = "CREATE TABLE IF NOT EXISTS app_categories ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "account_id INT NOT NULL, "
                + "parent_category_id BIGINT NULL, "
                + "name VARCHAR(80) NOT NULL, "
                + "icon_key VARCHAR(32) NOT NULL DEFAULT '◇', "
                + "root_pinned BOOLEAN NOT NULL DEFAULT FALSE, "
                + "sort_order INT NOT NULL DEFAULT 0, "
                + "deletion_batch_id CHAR(36) NULL, "
                + "deleted_at TIMESTAMP NULL DEFAULT NULL, "
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_categories_account FOREIGN KEY (account_id) "
                + "REFERENCES accounts(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) "
                + "REFERENCES app_categories(id) ON DELETE CASCADE, "
                + "INDEX ix_categories_account_parent "
                + "(account_id, parent_category_id, deleted_at, sort_order), "
                + "INDEX ix_categories_deletion_batch (account_id, deletion_batch_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createAccountsTable);
            statement.execute(createProfilesTable);
            statement.execute(createCategoriesTable);
            DatabaseMigrator.migrate(connection);
            System.out.println("[DB] База данных успешно синхронизирована. Таблицы готовы к работе.");
        } catch (SQLException exception) {
            System.err.println(
                    "[DB] КРИТИЧЕСКАЯ ОШИБКА при инициализации базы данных: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
        }
    }
}
