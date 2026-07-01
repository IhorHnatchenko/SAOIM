package org.example;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.W32APIOptions;

public class WindowsUtils {

    public interface ExtendedUser32 extends User32 {
        ExtendedUser32 INSTANCE = Native.load("user32", ExtendedUser32.class, W32APIOptions.DEFAULT_OPTIONS);

        // Объявляем функцию принудительно
        HWND GetShellWindow();
    }

    //Проверяет является ли текущее активное акно рабочим столом.
    public static boolean isDesktopActive(){
        HWND hwndForeground = ExtendedUser32.INSTANCE.GetForegroundWindow();

        HWND hwngShell = ExtendedUser32.INSTANCE.GetShellWindow();

        if (hwndForeground != null && hwndForeground.equals(hwngShell)){
            return true;
        }

        char[] className = new char[512];
        ExtendedUser32.INSTANCE.GetClassName(hwndForeground, className, 512);
        String name = Native.toString(className);

        return name.equals("Progman") || name.equals("WorkerW") || name.contains("Glass");
    }

    private static boolean isWorkerW(HWND hwnd){
        if (hwnd == null) return false;

        char[] className = new char[512];

        User32.INSTANCE.GetClassName(hwnd, className, 512);
        String name = Native.toString(className);

        return name.equals("Progman") || name.equals("WorkerW");
    }
}
