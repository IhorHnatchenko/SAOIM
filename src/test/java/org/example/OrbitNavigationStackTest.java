package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitNavigationStackTest {
    @Test
    void entersNestedCategoriesAndReturnsToRoot() {
        OrbitEntry java = TestDataFactory.orbitCategory(3, "Java", List.of());
        OrbitEntry development = TestDataFactory.orbitCategory(2, "Разработка", List.of(java));
        OrbitEntry work = TestDataFactory.orbitCategory(1, "Работа", List.of(development));
        OrbitNavigationStack stack = new OrbitNavigationStack(List.of(work));

        assertTrue(stack.isAtRoot());
        assertEquals("Корень категорий", stack.getBreadcrumb());
        assertTrue(stack.enter(work));
        assertTrue(stack.enter(development));
        assertTrue(stack.enter(java));

        assertEquals(3, stack.getDepth());
        assertEquals("Работа › Разработка › Java", stack.getBreadcrumb());
        assertEquals(List.of(1L, 2L, 3L), stack.getPathCategoryIds());

        assertTrue(stack.goBack());
        assertEquals("Разработка", stack.getCurrentTitle());
        stack.reset();
        assertTrue(stack.isAtRoot());
        assertNull(stack.getCurrentCategoryId());
    }

    @Test
    void ignoresShortcutAsNavigationTarget() {
        AppShortcut shortcut = TestDataFactory.shortcut(
                10, 1, 1, "Browser", LaunchType.URL, "https://example.com", 0, true
        );
        OrbitNavigationStack stack = new OrbitNavigationStack(List.of());

        assertFalse(stack.enter(OrbitEntry.shortcut(shortcut)));
        assertTrue(stack.isAtRoot());
    }

    @Test
    void restoresOnlyExistingPartOfPreviousPath() {
        OrbitEntry oldChild = TestDataFactory.orbitCategory(2, "Старая", List.of());
        OrbitEntry oldRoot = TestDataFactory.orbitCategory(1, "Работа", List.of(oldChild));
        OrbitNavigationStack stack = new OrbitNavigationStack(List.of(oldRoot));
        stack.enter(oldRoot);
        stack.enter(oldChild);

        OrbitEntry newRoot = TestDataFactory.orbitCategory(1, "Работа 2", List.of());
        stack.replaceRootEntries(List.of(newRoot), List.of(1L, 2L));

        assertEquals(1, stack.getDepth());
        assertEquals(Long.valueOf(1L), stack.getCurrentCategoryId());
        assertEquals("Работа 2", stack.getCurrentTitle());
    }
}
