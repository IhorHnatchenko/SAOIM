package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {
    @Test
    void acceptsUnicodeUsernameAndRejectsInvalidLengthOrSpaces() {
        assertTrue(PasswordValidator.isValidUsername("Игорь_2026"));
        assertTrue(PasswordValidator.isValidUsername("user.name"));
        assertFalse(PasswordValidator.isValidUsername("ab"));
        assertFalse(PasswordValidator.isValidUsername("name with spaces"));
        assertFalse(PasswordValidator.isValidUsername(null));
    }

    @Test
    void rejectsShortSequentialAndRepeatedPasswords() {
        assertTrue(PasswordValidator.isTooSimple("short"));
        assertTrue(PasswordValidator.isTooSimple("aaaaaaaaaaaa"));
        assertTrue(PasswordValidator.isTooSimple("abcdefghijkl"));
        assertTrue(PasswordValidator.isTooSimple("abcabcabcabc"));
    }

    @Test
    void acceptsLongNonRepeatingPassword() {
        assertFalse(PasswordValidator.isTooSimple("N7!vQ2@zR9#k"));
    }
}
