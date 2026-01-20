package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.ui.content.Content;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Utility class for finding terminal widgets.
 * Uses reflection to support different terminal implementations.
 */
public class TerminalWidgetFinder {

    @Nullable
    public static TerminalWidget findWidgetByContent(@NotNull Content content) {
        try {
            // Try to find the TerminalWidgetFinderUtil class via reflection
            Class<?> finderClass = Class.forName("org.jetbrains.plugins.terminal.TerminalWidgetFinderUtil");
            Method findMethod = finderClass.getMethod("findByContent", Content.class);
            return (TerminalWidget) findMethod.invoke(null, content);
        } catch (Exception e) {
            // Fallback: try to get component from content
            Object component = content.getComponent();
            if (component instanceof TerminalWidget) {
                return (TerminalWidget) component;
            }
            return null;
        }
    }
}

