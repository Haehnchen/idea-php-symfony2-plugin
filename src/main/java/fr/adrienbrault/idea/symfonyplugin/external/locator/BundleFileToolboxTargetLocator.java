package fr.adrienbrault.idea.symfonyplugin.external.locator;

import com.intellij.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleFileToolboxTargetLocator implements PhpToolboxTargetLocator {
    @NotNull
    @Override
    public Collection<PsiElement> getTargets(@NotNull TargetLocatorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        String target = parameter.getTarget();
        if(!target.startsWith("@")) {
            return Collections.emptyList();
        }

        return new ArrayList<>(FileResourceUtil.getFileResourceTargetsInBundleScope(parameter.getProject(), parameter.getTarget()));
    }
}
