package org.example;

import java.util.Optional;

/**
 * Monitor-related operations used by the global mouse gesture.
 *
 * JNativeHook reports physical desktop coordinates. Therefore the activation
 * corner is resolved through the native Windows monitor bounds instead of
 * assuming that every monitor starts at coordinate (0, 0).
 */
public final class ScreenService {

    public boolean isInTopLeftActivationZone(int screenX, int screenY, int zoneSize) {
        if (zoneSize <= 0) {
            return false;
        }

        Optional<MonitorBounds> monitor = WindowsUtils.getMonitorBoundsAt(screenX, screenY);
        return monitor
                .map(bounds -> bounds.containsTopLeftZone(screenX, screenY, zoneSize))
                .orElse(false);
    }
}
