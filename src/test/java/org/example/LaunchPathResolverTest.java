package org.example;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchPathResolverTest {
    @Test
    void removesOnlyOuterQuotesAndWhitespace() {
        assertEquals("C:\\Program Files\\App\\app.exe", LaunchPathResolver.normalize(
                "  \"C:\\Program Files\\App\\app.exe\"  "
        ));
    }

    @Test
    void resolvesRelativePathAsAbsoluteAndNormalized() {
        Path resolved = LaunchPathResolver.resolvePath(".");

        assertTrue(resolved.isAbsolute());
        assertEquals(Path.of(".").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void expandsHomePrefix() {
        Path resolved = LaunchPathResolver.resolvePath("~/saoim-test");
        Path expected = Path.of(System.getProperty("user.home"), "saoim-test")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, resolved);
    }

    @Test
    void leavesUnknownEnvironmentVariableUntouched() {
        String variable = "%SAOIM_TEST_VARIABLE_THAT_DOES_NOT_EXIST%";
        assertEquals(variable, LaunchPathResolver.expandEnvironmentVariables(variable));
    }
}
