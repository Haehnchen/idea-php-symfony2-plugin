package fr.adrienbrault.idea.symfony2plugin.external.locator;

import com.intellij.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
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
        String target = parameter.getTarget();
        if(!target.startsWith("@")) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new ArrayList<>();
        targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(parameter.getProject(), parameter.getTarget()));
        return targets;
    }
}
