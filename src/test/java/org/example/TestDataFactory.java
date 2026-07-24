package org.example;

import java.time.LocalDateTime;
import java.util.List;

final class TestDataFactory {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private TestDataFactory() {
    }

    static AppCategory category(
            long id,
            long accountId,
            Long parentId,
            String name,
            boolean pinned,
            int sortOrder
    ) {
        return new AppCategory(
                id,
                accountId,
                parentId,
                name,
                "◇",
                pinned,
                sortOrder,
                NOW,
                NOW
        );
    }

    static AppShortcut shortcut(
            long id,
            long accountId,
            long categoryId,
            String name,
            LaunchType type,
            String target,
            int sortOrder,
            boolean enabled
    ) {
        return shortcut(
                id,
                accountId,
                categoryId,
                name,
                type,
                target,
                null,
                null,
                sortOrder,
                enabled
        );
    }

    static AppShortcut shortcut(
            long id,
            long accountId,
            long categoryId,
            String name,
            LaunchType type,
            String target,
            String arguments,
            String workingDirectory,
            int sortOrder,
            boolean enabled
    ) {
        return new AppShortcut(
                id,
                accountId,
                categoryId,
                name,
                type,
                target,
                arguments,
                workingDirectory,
                null,
                sortOrder,
                enabled,
                NOW,
                NOW
        );
    }

    static OrbitEntry orbitCategory(long id, String title, List<OrbitEntry> children) {
        return OrbitEntry.category(
                id,
                title,
                "◇",
                "Тестовая категория",
                false,
                0,
                children
        );
    }
}
