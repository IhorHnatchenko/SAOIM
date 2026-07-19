package org.example;

/**
 * Physical Windows monitor bounds returned by the native user32 API.
 */
public record MonitorBounds(int left, int top, int right, int bottom) {

    public MonitorBounds {
        if (right <= left) {
            throw new IllegalArgumentException("right must be greater than left");
        }
        if (bottom <= top) {
            throw new IllegalArgumentException("bottom must be greater than top");
        }
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public boolean contains(int x, int y) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    public boolean containsTopLeftZone(int x, int y, int zoneSize) {
        if (zoneSize <= 0) {
            return false;
        }

        return x >= left
                && x <= left + zoneSize
                && y >= top
                && y <= top + zoneSize;
    }
}
