package org.example;

import java.time.LocalDateTime;
import java.util.Objects;

/** Database model of one account-owned launch shortcut. */
public final class AppShortcut {
    private final long id;
    private final long accountId;
    private final long categoryId;
    private final String displayName;
    private final LaunchType launchType;
    private final String target;
    private final String arguments;
    private final String workingDirectory;
    private final String iconSource;
    private final int sortOrder;
    private final boolean enabled;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AppShortcut(
            long id,
            long accountId,
            long categoryId,
            String displayName,
            LaunchType launchType,
            String target,
            String arguments,
            String workingDirectory,
            String iconSource,
            int sortOrder,
            boolean enabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException("Shortcut id must be positive");
        }
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account id must be positive");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category id must be positive");
        }
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.displayName = normalize(displayName, "Без названия");
        this.launchType = Objects.requireNonNullElse(launchType, LaunchType.EXECUTABLE);
        this.target = normalize(target, "");
        this.arguments = normalizeNullable(arguments);
        this.workingDirectory = normalizeNullable(workingDirectory);
        this.iconSource = normalizeNullable(iconSource);
        this.sortOrder = Math.max(0, sortOrder);
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LaunchType getLaunchType() {
        return launchType;
    }

    public String getTarget() {
        return target;
    }

    public String getArguments() {
        return arguments;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getIconSource() {
        return iconSource;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Until IconService is implemented at step 8, a short icon source is treated
     * as an emoji/key. Long paths fall back to the launch-type icon.
     */
    public String getDisplayIcon() {
        if (iconSource == null || iconSource.isBlank() || iconSource.length() > 16
                || iconSource.contains("\\") || iconSource.contains("/")) {
            return launchType.getDefaultIcon();
        }
        return iconSource;
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
