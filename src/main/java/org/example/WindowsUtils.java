package org.example;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;

import java.util.Optional;

/**
 * Small, Windows-specific native helpers.
 */
public final class WindowsUtils {

    private static final int CLASS_NAME_BUFFER_SIZE = 512;

    private WindowsUtils() {
    }

    public interface ExtendedUser32 extends User32 {
        ExtendedUser32 INSTANCE = Native.load(
                "user32",
                ExtendedUser32.class,
                W32APIOptions.DEFAULT_OPTIONS
        );

        HWND GetShellWindow();
    }

    /**
     * Returns true only for the Windows desktop context. JavaFX/Glass windows
     * are intentionally not treated as the desktop; visible SAOIM states are
     * handled explicitly by GestureRouter.
     */
    public static boolean isDesktopActive() {
        HWND foregroundWindow = ExtendedUser32.INSTANCE.GetForegroundWindow();
        if (foregroundWindow == null) {
            return false;
        }

        HWND shellWindow = ExtendedUser32.INSTANCE.GetShellWindow();
        if (shellWindow != null && foregroundWindow.equals(shellWindow)) {
            return true;
        }

        String className = getWindowClassName(foregroundWindow);
        return "Progman".equals(className) || "WorkerW".equals(className);
    }

    /**
     * Resolves the physical monitor rectangle containing the supplied global
     * mouse coordinates.
     */
    public static Optional<MonitorBounds> getMonitorBoundsAt(int screenX, int screenY) {
        WinDef.POINT.ByValue point = new WinDef.POINT.ByValue();
        point.x = screenX;
        point.y = screenY;

        WinUser.HMONITOR monitor = User32.INSTANCE.MonitorFromPoint(
                point,
                WinUser.MONITOR_DEFAULTTONULL
        );
        if (monitor == null) {
            return Optional.empty();
        }

        WinUser.MONITORINFO monitorInfo = new WinUser.MONITORINFO();
        monitorInfo.cbSize = monitorInfo.size();

        WinDef.BOOL result = User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo);
        if (result == null || !result.booleanValue()) {
            return Optional.empty();
        }

        WinDef.RECT rectangle = monitorInfo.rcMonitor;
        if (rectangle == null || rectangle.right <= rectangle.left || rectangle.bottom <= rectangle.top) {
            return Optional.empty();
        }

        return Optional.of(new MonitorBounds(
                rectangle.left,
                rectangle.top,
                rectangle.right,
                rectangle.bottom
        ));
    }

    private static String getWindowClassName(HWND window) {
        if (window == null) {
            return "";
        }

        char[] className = new char[CLASS_NAME_BUFFER_SIZE];
        int copiedCharacters = ExtendedUser32.INSTANCE.GetClassName(
                window,
                className,
                className.length
        );

        return copiedCharacters > 0 ? Native.toString(className) : "";
    }
}
