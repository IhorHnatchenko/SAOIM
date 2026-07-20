package org.example;

import java.util.Objects;

/**
 * Immutable profile data used by the profile UI.
 *
 * Step 3 still uses starter values, but keeping them in a model prevents
 * ProfileCard from owning business data and prepares the UI for database
 * loading in step 5.
 */
public final class UserProfile {

    public static final String DEFAULT_TITLE = "Энтузиаст";
    public static final int DEFAULT_LEVEL = 1;
    public static final int DEFAULT_CURRENT_XP = 0;
    public static final int DEFAULT_REQUIRED_XP = 1000;

    private final long accountId;
    private final String nickname;
    private final String title;
    private final int level;
    private final int currentXp;
    private final int requiredXp;
    private final String avatarUri;

    public UserProfile(
            long accountId,
            String nickname,
            String title,
            int level,
            int currentXp,
            int requiredXp,
            String avatarUri
    ) {
        this.accountId = accountId;
        this.nickname = normalize(nickname, "Guest");
        this.title = normalize(title, DEFAULT_TITLE);
        this.level = Math.max(1, level);
        this.currentXp = Math.max(0, currentXp);
        this.requiredXp = Math.max(1, requiredXp);
        this.avatarUri = avatarUri;
    }

    public static UserProfile starter(String nickname) {
        return new UserProfile(
                -1,
                nickname,
                DEFAULT_TITLE,
                DEFAULT_LEVEL,
                DEFAULT_CURRENT_XP,
                DEFAULT_REQUIRED_XP,
                null
        );
    }

    public long getAccountId() {
        return accountId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getTitle() {
        return title;
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public int getRequiredXp() {
        return requiredXp;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public double getXpProgress() {
        return Math.min(1.0, (double) currentXp / requiredXp);
    }

    public UserProfile withNickname(String nickname) {
        return new UserProfile(
                accountId,
                nickname,
                title,
                level,
                currentXp,
                requiredXp,
                avatarUri
        );
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
