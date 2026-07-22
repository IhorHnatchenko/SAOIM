package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class AuthService {
    private static final AtomicReference<UserSession> LAST_AUTHENTICATED_SESSION =
            new AtomicReference<>();

    private AuthService() {
    }

    /** Compatibility method used by the current AuthWindow. */
    public static boolean loginUser(String username, String password) {
        return authenticate(username, password).isPresent();
    }

    public static Optional<UserSession> authenticate(String username, String password) {
        LAST_AUTHENTICATED_SESSION.set(null);
        String query = "SELECT a.id, a.username, a.password_hash, p.sao_id "
                + "FROM accounts a "
                + "LEFT JOIN profiles p ON p.account_id = a.id "
                + "WHERE a.username = ? LIMIT 1";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String storedHash = resultSet.getString("password_hash");
                if (!BCrypt.checkpw(password, storedHash)) {
                    return Optional.empty();
                }
                UserSession session = new UserSession(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("sao_id")
                );
                LAST_AUTHENTICATED_SESSION.set(session);
                return Optional.of(session);
            }
        } catch (SQLException exception) {
            System.err.println("[Auth] Ошибка авторизации в MySQL: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<UserSession> getLastAuthenticatedSession() {
        return Optional.ofNullable(LAST_AUTHENTICATED_SESSION.get());
    }

    public static void clearAuthenticatedSession() {
        LAST_AUTHENTICATED_SESSION.set(null);
    }

    public static boolean registerUser(
            String username,
            String email,
            String password,
            String country,
            String city
    ) throws RegistrationException {
        String insertAccountSql =
                "INSERT INTO accounts(username, email, password_hash) VALUES(?, ?, ?)";
        String insertProfileSql = "INSERT INTO profiles("
                + "account_id, sao_id, country, city, nickname, title, level, current_xp, required_xp"
                + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            int accountId = -1;

            try (PreparedStatement statement = connection.prepareStatement(
                    insertAccountSql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, hashedPassword);
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        accountId = generatedKeys.getInt(1);
                    }
                }
            }

            if (accountId == -1) {
                throw new SQLException("Не удалось получить ID созданного аккаунта из MySQL.");
            }

            String saoId = generateSaoId();
            try (PreparedStatement statement = connection.prepareStatement(insertProfileSql)) {
                statement.setInt(1, accountId);
                statement.setString(2, saoId);
                statement.setString(3, country);
                statement.setString(4, city);
                statement.setString(5, username);
                statement.setString(6, UserProfile.DEFAULT_TITLE);
                statement.setInt(7, UserProfile.DEFAULT_LEVEL);
                statement.setInt(8, UserProfile.DEFAULT_CURRENT_XP);
                statement.setInt(9, UserProfile.DEFAULT_REQUIRED_XP);
                statement.executeUpdate();
            }

            new CategoryRepository().createDefaultCategories(connection, accountId);
            connection.commit();
            System.out.println("[Auth] Пользователь зарегистрирован в MySQL. SAO ID: " + saoId);
            return true;
        } catch (SQLException exception) {
            rollbackQuietly(connection);
            if (exception.getErrorCode() == 1062) {
                String errorMessage = exception.getMessage().toLowerCase();
                if (errorMessage.contains("email")) {
                    throw new RegistrationException("EMAIL ALREADY EXISTS!");
                }
                if (errorMessage.contains("username")) {
                    throw new RegistrationException("USERNAME TAKEN!");
                }
            }
            System.err.println("[Auth] Ошибка транзакции MySQL: " + exception.getMessage());
            throw new RegistrationException("SYNC FAILED!");
        } finally {
            closeQuietly(connection);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static String generateSaoId() {
        return "SAO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
