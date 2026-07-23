package org.example;

/** Strategy used by AppLauncher for one or more shortcut target types. */
public interface LaunchStrategy {
    LaunchResult launch(AppShortcut shortcut) throws Exception;
}
