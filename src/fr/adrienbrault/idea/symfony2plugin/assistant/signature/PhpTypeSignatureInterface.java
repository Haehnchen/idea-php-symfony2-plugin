package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface PhpTypeSignatureInterface {

    @Nullable
    public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter);

    @NotNull
    public String getName();
}
