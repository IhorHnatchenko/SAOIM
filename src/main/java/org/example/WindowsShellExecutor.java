package org.example;

import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/** Thin, safe wrapper around Windows ShellExecute. */
public final class WindowsShellExecutor {
    public LaunchResult open(
            String target,
            String parameters,
            String workingDirectory,
            String successMessage
    ) {
        if (!ShortcutAvailabilityService.isWindows()) {
            return LaunchResult.failure("Эта операция поддерживается только в Windows.");
        }
        try {
            WinDef.INT_PTR result = Shell32.INSTANCE.ShellExecute(
                    null,
                    "open",
                    target,
                    blankToNull(parameters),
                    blankToNull(workingDirectory),
                    WinUser.SW_SHOWNORMAL
            );
            long code = result == null ? 0 : result.longValue();
            if (code <= 32) {
                return LaunchResult.failure(shellErrorMessage(code, target));
            }
            return LaunchResult.success(successMessage);
        } catch (Throwable throwable) {
            return LaunchResult.failure(
                    "Windows Shell не смог открыть цель: " + safeMessage(throwable),
                    throwable
            );
        }
    }

    private String shellErrorMessage(long code, String target) {
        String reason = switch ((int) code) {
            case 0, 8 -> "недостаточно памяти или системных ресурсов";
            case 2 -> "файл не найден";
            case 3 -> "путь не найден";
            case 5 -> "доступ запрещён";
            case 26 -> "нарушение совместного доступа";
            case 27 -> "неполная ассоциация файла";
            case 28 -> "истекло время ожидания DDE";
            case 29 -> "ошибка DDE";
            case 30 -> "DDE занято";
            case 31 -> "для типа файла не найдено связанное приложение";
            case 32 -> "не найдена необходимая DLL";
            default -> "код Windows Shell " + code;
        };
        return "Не удалось открыть «" + target + "»: " + reason + ".";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }
}
