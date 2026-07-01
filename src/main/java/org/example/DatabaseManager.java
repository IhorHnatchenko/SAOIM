package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final String URL = dotenv.get("DB_URL", "jdbc:mysql://127.0.0.1:3307/sao_server?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String USER = dotenv.get("DB_USER", "root");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Драйвер MySQL не найден: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initDatabase() {
        System.out.println("[DB] Инициализация соединения с базой данных...");

        String createAccountsTable =
                "CREATE TABLE IF NOT EXISTS accounts (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(32) NOT NULL UNIQUE, " +
                        "email VARCHAR(255) NOT NULL UNIQUE, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createProfilesTable =
                "CREATE TABLE IF NOT EXISTS profiles (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "account_id INT NOT NULL, " +
                        "sao_id VARCHAR(50) NOT NULL UNIQUE, " +
                        "country VARCHAR(100), " +
                        "city VARCHAR(100), " +
                        "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createAccountsTable);
            stmt.execute(createProfilesTable);

            System.out.println("[DB] База данных успешно синхронизирована. Таблицы готовы к работе.");

        } catch (SQLException e) {
            System.err.println("[DB] КРИТИЧЕСКАЯ ОШИБКА при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}