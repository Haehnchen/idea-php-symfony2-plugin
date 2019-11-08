package fr.adrienbrault.idea.symfony2plugin.routing.dic;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerClassOnShortcutReturn {

    @NotNull
    private final PhpClass phpClass;
    private boolean isService = false;

    public ControllerClassOnShortcutReturn(@NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public ControllerClassOnShortcutReturn(@NotNull PhpClass phpClass, boolean isService) {
        this.phpClass = phpClass;
        this.isService = isService;
    }

    public boolean isService() {
        return isService;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

}
