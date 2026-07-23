package org.example;

import java.awt.Desktop;
import java.net.URI;

/** Opens HTTP/HTTPS links in the user's default browser. */
public final class UrlLaunchStrategy implements LaunchStrategy {
    private final WindowsShellExecutor shellExecutor;

    public UrlLaunchStrategy(WindowsShellExecutor shellExecutor) {
        this.shellExecutor = shellExecutor == null
                ? new WindowsShellExecutor()
                : shellExecutor;
    }

    @Override
    public LaunchResult launch(AppShortcut shortcut) throws Exception {
        URI uri = URI.create(shortcut.getTarget().trim());
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return LaunchResult.success("Открыт URL: " + shortcut.getDisplayName());
        }
        return shellExecutor.open(
                uri.toString(),
                null,
                null,
                "Открыт URL: " + shortcut.getDisplayName()
        );
    }
}
