package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommand {
    @NotNull
    private final String name;

    @NotNull
    private final PhpClass phpClass;

    public SymfonyCommand(@NotNull String name, @NotNull PhpClass phpClass) {
        this.name = name;
        this.phpClass = phpClass;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }
}
