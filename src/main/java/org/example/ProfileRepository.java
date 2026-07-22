package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Database access for profile data. No JavaFX code belongs in this class. */
public final class ProfileRepository {
    public UserProfile findByAccountId(long accountId, String fallbackUsername) throws SQLException {
        if (accountId <= 0) {
            return UserProfile.starter(fallbackUsername);
        }

        String sql = "SELECT " +
                "a.id AS account_id, a.username, " +
                "p.nickname, p.title, p.level, p.current_xp, " +
                "p.required_xp, p.avatar_uri " +
                "FROM accounts a " +
                "LEFT JOIN profiles p ON p.account_id = a.id " +
                "WHERE a.id = ? " +
                "LIMIT 1";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Аккаунт с ID " + accountId + " не найден.");
                }

                String username = firstNotBlank(
                        resultSet.getString("nickname"),
                        resultSet.getString("username"),
                        fallbackUsername,
                        "Guest"
                );

                return new UserProfile(
                        resultSet.getLong("account_id"),
                        username,
                        resultSet.getString("title"),
                        resultSet.getInt("level"),
                        resultSet.getInt("current_xp"),
                        resultSet.getInt("required_xp"),
                        resultSet.getString("avatar_uri")
                );
            }
        }
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "Guest";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "Guest";
    }
}
