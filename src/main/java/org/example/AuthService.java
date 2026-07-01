package org.example;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.UUID;

public class AuthService {

    public static boolean loginUser(String username, String password) {
        String query = "SELECT password_hash FROM accounts WHERE username = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    return BCrypt.checkpw(password, storedHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Auth] Ошибка авторизации в MySQL: " + e.getMessage());
        }
        return false;
    }

    public static boolean registerUser(String username, String email, String password, String country, String city) throws RegistrationException {
        String insertAccountSQL = "INSERT INTO accounts(username, email, password_hash) VALUES(?, ?, ?);";
        String insertProfileSQL = "INSERT INTO profiles(account_id, sao_id, country, city) VALUES(?, ?, ?, ?);";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            int accountId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertAccountSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, hashedPassword);
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        accountId = generatedKeys.getInt(1);
                    }
                }
            }

            if (accountId == -1) {
                throw new SQLException("Не удалось получить ID созданного аккаунта из MySQL.");
            }

            String saoId = generateSaoId();

            try (PreparedStatement pstmt = conn.prepareStatement(insertProfileSQL)) {
                pstmt.setInt(1, accountId);
                pstmt.setString(2, saoId);
                pstmt.setString(3, country);
                pstmt.setString(4, city);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println("[Auth] Пользователь зарегистрирован в MySQL. SAO ID: " + saoId);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }

            // Перехватываем дубликаты уникальных ключей MySQL (Код ошибки 1062)
            if (e.getErrorCode() == 1062) {
                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("email")) {
                    throw new RegistrationException("EMAIL ALREADY EXISTS!");
                } else if (errorMsg.contains("username")) {
                    throw new RegistrationException("USERNAME TAKEN!");
                }
            }

            System.err.println("[Auth] Ошибка транзакции MySQL: " + e.getMessage());
            throw new RegistrationException("SYNC FAILED!");
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private static String generateSaoId() {
        return "SAO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}