package org.example;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * Extracts native Windows file icons and caches them by source path.
 * Expensive shell/icon work is performed outside JavaFX Application Thread.
 */
public final class IconService {
    private static final int ICON_SIZE = 64;
    private static final Map<String, CompletableFuture<Image>> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService ICON_EXECUTOR = Executors.newFixedThreadPool(
            2,
            new IconThreadFactory()
    );

    public void loadIconAsync(AppShortcut shortcut, Consumer<Image> onLoaded) {
        if (shortcut == null || onLoaded == null) {
            return;
        }
        String key = cacheKey(shortcut);
        if (key == null) {
            return;
        }

        CompletableFuture<Image> future = CACHE.computeIfAbsent(
                key,
                ignored -> CompletableFuture.supplyAsync(
                        () -> loadIcon(shortcut),
                        ICON_EXECUTOR
                )
        );
        future.whenComplete((image, error) -> {
            if (error != null || image == null || image.isError()) {
                return;
            }
            runOnJavaFxThread(() -> onLoaded.accept(image));
        });
    }

    public void clearCache() {
        CACHE.clear();
    }

    private Image loadIcon(AppShortcut shortcut) {
        Path customSource = resolveExistingPath(shortcut.getIconSource());
        if (customSource != null) {
            Image customImage = loadCustomImage(customSource);
            if (customImage != null && !customImage.isError()) {
                return customImage;
            }
            Image customSystemIcon = loadSystemIcon(customSource);
            if (customSystemIcon != null) {
                return customSystemIcon;
            }
        }

        if (isFileTarget(shortcut.getLaunchType())) {
            Path target = resolveExistingPath(shortcut.getTarget());
            if (target != null) {
                return loadSystemIcon(target);
            }
        }
        return null;
    }

    private Image loadCustomImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean supported = name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".gif")
                || name.endsWith(".bmp");
        if (!supported || !Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream input = Files.newInputStream(path)) {
            return new Image(input, ICON_SIZE, ICON_SIZE, true, true);
        } catch (Exception exception) {
            System.err.println("[Icons] Не удалось загрузить " + path + ": "
                    + exception.getMessage());
            return null;
        }
    }

    private Image loadSystemIcon(Path path) {
        try {
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(
                    path.toFile(),
                    ICON_SIZE,
                    ICON_SIZE
            );
            if (icon == null) {
                return null;
            }
            BufferedImage buffered = new BufferedImage(
                    Math.max(1, icon.getIconWidth()),
                    Math.max(1, icon.getIconHeight()),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = buffered.createGraphics();
            try {
                icon.paintIcon(null, graphics, 0, 0);
            } finally {
                graphics.dispose();
            }
            return toJavaFxImage(buffered);
        } catch (Throwable throwable) {
            System.err.println("[Icons] Не удалось извлечь системную иконку " + path
                    + ": " + safeMessage(throwable));
            return null;
        }
    }

    private Image toJavaFxImage(BufferedImage source) {
        WritableImage target = new WritableImage(source.getWidth(), source.getHeight());
        int[] pixels = source.getRGB(
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                null,
                0,
                source.getWidth()
        );
        target.getPixelWriter().setPixels(
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                pixels,
                0,
                source.getWidth()
        );
        return target;
    }

    private Path resolveExistingPath(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            Path path = LaunchPathResolver.resolvePath(rawValue);
            return Files.exists(path) ? path : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isFileTarget(LaunchType launchType) {
        return launchType == LaunchType.EXECUTABLE
                || launchType == LaunchType.WINDOWS_SHORTCUT
                || launchType == LaunchType.FILE
                || launchType == LaunchType.DIRECTORY;
    }

    private String cacheKey(AppShortcut shortcut) {
        String iconSource = shortcut.getIconSource();
        if (iconSource != null && !iconSource.isBlank()
                && (iconSource.contains("\\") || iconSource.contains("/"))) {
            return "custom:" + LaunchPathResolver.expandEnvironmentVariables(iconSource.trim());
        }
        if (isFileTarget(shortcut.getLaunchType())) {
            return "target:" + LaunchPathResolver.expandEnvironmentVariables(
                    shortcut.getTarget().trim()
            );
        }
        return null;
    }

    private void runOnJavaFxThread(Runnable runnable) {
        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                Platform.runLater(runnable);
            }
        } catch (IllegalStateException exception) {
            // Useful for isolated unit tests before the JavaFX toolkit is started.
            runnable.run();
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private static final class IconThreadFactory implements ThreadFactory {
        private int counter;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "saoim-icon-loader-" + (++counter));
            thread.setDaemon(true);
            return thread;
        }
    }
}
