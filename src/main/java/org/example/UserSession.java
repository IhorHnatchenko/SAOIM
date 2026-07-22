package org.example;

import java.util.Objects;

/**
 * Authenticated account context shared by the overlay and repositories.
 *
 * The session intentionally contains only lightweight identity data. The
 * profile itself is loaded asynchronously by ProfileRepository so that login
 * and JavaFX rendering are not blocked by an additional database query.
 */
public final class UserSession {
    private static final UserSession GUEST = new UserSession(-1L, "Guest", null);

    private final long accountId;
    private final String username;
    private final String saoId;

    public UserSession(long accountId, String username, String saoId) {
        this.accountId = accountId;
        this.username = normalize(username, "Guest");
        this.saoId = normalizeNullable(saoId);
    }

    public static UserSession guest() {
        return GUEST;
    }

    public static UserSession guest(String username) {
        return new UserSession(-1L, username, null);
    }

    public long getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getSaoId() {
        return saoId;
    }

    public boolean isAuthenticated() {
        return accountId > 0;
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String normalizeNullable(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "accountId=" + accountId +
                ", username='" + username + '\'' +
                ", saoId='" + saoId + '\'' +
                '}';
    }
}
