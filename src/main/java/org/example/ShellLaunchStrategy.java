package org.example;

import java.nio.file.Path;

/** Opens .lnk files, ordinary files and directories through Windows Shell. */
public final class ShellLaunchStrategy implements LaunchStrategy {
    private final WindowsShellExecutor shellExecutor;

    public ShellLaunchStrategy(WindowsShellExecutor shellExecutor) {
        this.shellExecutor = shellExecutor == null
                ? new WindowsShellExecutor()
                : shellExecutor;
    }

    @Override
    public LaunchResult launch(AppShortcut shortcut) {
        Path target = LaunchPathResolver.resolvePath(shortcut.getTarget());
        String workingDirectory = shortcut.getWorkingDirectory();
        if ((workingDirectory == null || workingDirectory.isBlank())
                && target.getParent() != null) {
            workingDirectory = target.getParent().toString();
        }
        String parameters = shortcut.getLaunchType() == LaunchType.DIRECTORY
                ? null
                : shortcut.getArguments();
        return shellExecutor.open(
                target.toString(),
                parameters,
                workingDirectory,
                "Открыто: " + shortcut.getDisplayName()
        );
    }
}
