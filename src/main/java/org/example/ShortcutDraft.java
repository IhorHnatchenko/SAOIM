package org.example;

/** Values entered in the shortcut editor before validation and persistence. */
public record ShortcutDraft(
        String displayName,
        LaunchType launchType,
        String target,
        String arguments,
        String workingDirectory,
        String iconSource,
        boolean enabled
) {
}
