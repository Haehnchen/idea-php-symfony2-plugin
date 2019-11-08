package fr.adrienbrault.idea.symfonyplugin.util.controller;

import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerAction {
    @NotNull
    final private String shortcutName;

    @NotNull
    final private Method method;

    public ControllerAction(@NotNull String shortcutName, @NotNull Method method) {
       this.shortcutName = shortcutName;
       this.method = method;
    }

    @NotNull
    public Method getMethod() {
        return method;
    }

    @NotNull
    public String getShortcutName() {
        return shortcutName;
    }
}

