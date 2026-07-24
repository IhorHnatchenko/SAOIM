package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutAvailabilityServiceTest {
    private final ShortcutAvailabilityService service = new ShortcutAvailabilityService();

    @TempDir
    Path tempDirectory;

    @Test
    void acceptsExistingFileAndDirectory() throws IOException {
        Path file = Files.createFile(tempDirectory.resolve("document.txt"));
        Path directory = Files.createDirectory(tempDirectory.resolve("folder"));

        LaunchAvailability fileResult = service.check(TestDataFactory.shortcut(
                1, 10, 100, "Документ", LaunchType.FILE, file.toString(), 0, true
        ));
        LaunchAvailability directoryResult = service.check(TestDataFactory.shortcut(
                2, 10, 100, "Папка", LaunchType.DIRECTORY, directory.toString(), 1, true
        ));

        assertTrue(fileResult.available(), fileResult.message());
        assertTrue(directoryResult.available(), directoryResult.message());
    }

    @Test
    void validatesExeAndWindowsShortcutExtensions() throws IOException {
        Path exe = Files.createFile(tempDirectory.resolve("tool.exe"));
        Path lnk = Files.createFile(tempDirectory.resolve("tool.lnk"));
        Path wrong = Files.createFile(tempDirectory.resolve("tool.txt"));

        assertTrue(service.check(TestDataFactory.shortcut(
                1, 10, 100, "EXE", LaunchType.EXECUTABLE, exe.toString(), 0, true
        )).available());
        assertTrue(service.check(TestDataFactory.shortcut(
                2, 10, 100, "LNK", LaunchType.WINDOWS_SHORTCUT, lnk.toString(), 0, true
        )).available());
        assertFalse(service.check(TestDataFactory.shortcut(
                3, 10, 100, "Wrong", LaunchType.EXECUTABLE, wrong.toString(), 0, true
        )).available());
    }

    @Test
    void rejectsMissingWorkingDirectory() throws IOException {
        Path exe = Files.createFile(tempDirectory.resolve("tool.exe"));
        Path missingDirectory = tempDirectory.resolve("missing");

        LaunchAvailability result = service.check(TestDataFactory.shortcut(
                1,
                10,
                100,
                "EXE",
                LaunchType.EXECUTABLE,
                exe.toString(),
                "--safe",
                missingDirectory.toString(),
                0,
                true
        ));

        assertFalse(result.available());
        assertTrue(result.message().contains("Рабочая папка"));
    }

    @Test
    void acceptsOnlyHttpAndHttpsUrls() {
        assertTrue(service.check(TestDataFactory.shortcut(
                1, 10, 100, "Web", LaunchType.URL, "https://example.com/path", 0, true
        )).available());
        assertTrue(service.check(TestDataFactory.shortcut(
                2, 10, 100, "Web", LaunchType.URL, "http://localhost:8080", 0, true
        )).available());
        assertFalse(service.check(TestDataFactory.shortcut(
                3, 10, 100, "FTP", LaunchType.URL, "ftp://example.com", 0, true
        )).available());
    }

    @Test
    void rejectsDisabledOrMissingTargetsWithoutThrowing() {
        assertFalse(service.check(TestDataFactory.shortcut(
                1, 10, 100, "Disabled", LaunchType.FILE, "missing.txt", 0, false
        )).available());
        assertFalse(service.check(TestDataFactory.shortcut(
                2, 10, 100, "Blank", LaunchType.FILE, "  ", 0, true
        )).available());
        assertFalse(service.check(null).available());
    }
}
