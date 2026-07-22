package org.example;

import java.time.LocalDateTime;
import java.util.Objects;

/** Database model for one user-owned category. */
public final class AppCategory {
    private final long id;
    private final long accountId;
    private final Long parentCategoryId;
    private final String name;
    private final String iconKey;
    private final boolean rootPinned;
    private final int sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AppCategory(
            long id,
            long accountId,
            Long parentCategoryId,
            String name,
            String iconKey,
            boolean rootPinned,
            int sortOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException("Category id must be positive");
        }
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account id must be positive");
        }
        if (parentCategoryId != null && parentCategoryId <= 0) {
            throw new IllegalArgumentException("Parent category id must be positive or null");
        }
        this.id = id;
        this.accountId = accountId;
        this.parentCategoryId = parentCategoryId;
        this.name = normalize(name, "Без названия");
        this.iconKey = normalize(iconKey, "◇");
        this.rootPinned = parentCategoryId == null && rootPinned;
        this.sortOrder = Math.max(0, sortOrder);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public boolean isRootPinned() {
        return rootPinned;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isRoot() {
        return parentCategoryId == null;
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    @Override
    public String toString() {
        return name;
    }
}
