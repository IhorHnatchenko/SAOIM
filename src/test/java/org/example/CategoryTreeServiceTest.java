package org.example;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTreeServiceTest {
    private static final long ACCOUNT_ID = 42;
    private final CategoryTreeService service = new CategoryTreeService();

    @Test
    void buildsMixedCategoryAndShortcutOrder() {
        AppCategory root = TestDataFactory.category(1, ACCOUNT_ID, null, "Работа", true, 0);
        AppCategory categoryLater = TestDataFactory.category(2, ACCOUNT_ID, 1L, "Документы", false, 20);
        AppCategory categoryFirst = TestDataFactory.category(3, ACCOUNT_ID, 1L, "Разработка", false, 10);
        AppShortcut sameOrderShortcut = TestDataFactory.shortcut(
                10, ACCOUNT_ID, 1, "GitHub", LaunchType.URL, "https://github.com", 10, true
        );
        AppShortcut middleShortcut = TestDataFactory.shortcut(
                11, ACCOUNT_ID, 1, "IDE", LaunchType.FILE, "idea.txt", 15, true
        );

        CategorySnapshot snapshot = service.buildSnapshot(
                ACCOUNT_ID,
                List.of(root, categoryLater, categoryFirst),
                List.of(middleShortcut, sameOrderShortcut)
        );

        assertEquals(1, snapshot.getPinnedRootEntries().size());
        List<OrbitEntry> children = snapshot.getAllRootEntries().get(0).getChildren();
        assertEquals(
                List.of("Разработка", "GitHub", "IDE", "Документы"),
                children.stream().map(OrbitEntry::getTitle).toList()
        );
    }

    @Test
    void rejectsCycleMissingParentAndForeignAccountData() {
        AppCategory cycleOne = TestDataFactory.category(1, ACCOUNT_ID, 2L, "One", false, 0);
        AppCategory cycleTwo = TestDataFactory.category(2, ACCOUNT_ID, 1L, "Two", false, 0);
        assertThrows(
                IllegalStateException.class,
                () -> service.buildSnapshot(ACCOUNT_ID, List.of(cycleOne, cycleTwo))
        );

        AppCategory missingParent = TestDataFactory.category(3, ACCOUNT_ID, 999L, "Missing", false, 0);
        assertThrows(
                IllegalStateException.class,
                () -> service.buildSnapshot(ACCOUNT_ID, List.of(missingParent))
        );

        AppCategory foreign = TestDataFactory.category(4, 777, null, "Foreign", false, 0);
        assertThrows(
                IllegalStateException.class,
                () -> service.buildSnapshot(ACCOUNT_ID, List.of(foreign))
        );
    }

    @Test
    void preventsMovingCategoryIntoItselfOrDescendant() {
        List<AppCategory> categories = List.of(
                TestDataFactory.category(1, ACCOUNT_ID, null, "Root", true, 0),
                TestDataFactory.category(2, ACCOUNT_ID, 1L, "Child", false, 0),
                TestDataFactory.category(3, ACCOUNT_ID, 2L, "Grandchild", false, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateMove(ACCOUNT_ID, categories, 1, 1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateMove(ACCOUNT_ID, categories, 1, 3L)
        );

        Set<Long> descendants = service.collectDescendantIds(categories, 1);
        assertEquals(Set.of(2L, 3L), descendants);
    }

    @Test
    void handlesDeepTreeWithoutLosingNavigationData() {
        int depth = 300;
        List<AppCategory> categories = new ArrayList<>(depth);
        for (int i = 1; i <= depth; i++) {
            categories.add(TestDataFactory.category(
                    i,
                    ACCOUNT_ID,
                    i == 1 ? null : (long) i - 1,
                    "Level " + i,
                    i == 1,
                    0
            ));
        }

        CategorySnapshot snapshot = assertTimeout(
                Duration.ofSeconds(5),
                () -> service.buildSnapshot(ACCOUNT_ID, categories)
        );

        OrbitEntry current = snapshot.getAllRootEntries().get(0);
        int visited = 1;
        while (!current.getChildren().isEmpty()) {
            current = current.getChildren().get(0);
            visited++;
        }
        assertEquals(depth, visited);
    }

    @Test
    void buildsLargeFlatDatasetWithinReasonableTime() {
        int categoryCount = 1_000;
        int shortcutCount = 1_000;
        List<AppCategory> categories = new ArrayList<>(categoryCount + 1);
        List<AppShortcut> shortcuts = new ArrayList<>(shortcutCount);
        categories.add(TestDataFactory.category(1, ACCOUNT_ID, null, "Load root", true, 0));

        for (int i = 1; i <= categoryCount; i++) {
            categories.add(TestDataFactory.category(
                    i + 1L,
                    ACCOUNT_ID,
                    1L,
                    "Category " + i,
                    false,
                    i
            ));
        }
        for (int i = 1; i <= shortcutCount; i++) {
            shortcuts.add(TestDataFactory.shortcut(
                    i,
                    ACCOUNT_ID,
                    1,
                    "Shortcut " + i,
                    LaunchType.URL,
                    "https://example.com/" + i,
                    categoryCount + i,
                    true
            ));
        }

        CategorySnapshot snapshot = assertTimeout(
                Duration.ofSeconds(5),
                () -> service.buildSnapshot(ACCOUNT_ID, categories, shortcuts)
        );

        assertEquals(categoryCount + shortcutCount, snapshot.getAllRootEntries().get(0).getChildren().size());
        assertTrue(snapshot.getAllRootEntries().get(0).getDescription().contains("ярлыков"));
    }
}
