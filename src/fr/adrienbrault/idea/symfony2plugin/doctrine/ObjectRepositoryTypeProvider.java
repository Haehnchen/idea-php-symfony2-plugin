package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.SymfonyInterfacesHelper;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryTypeProvider implements PhpTypeProvider {


    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }

       if (!SymfonyInterfacesHelper.isRepositoryCall(e)) {
            return null;
        }

        String repositoryName = SymfonyInterfacesHelper.getFirstArgumentStringValue((MethodReference) e);
        if (null == repositoryName) {
            return null;
        }

        // @TODO: parse xml or yml for repositoryClass?
        PhpClass phpClass = EntityHelper.resolveShortcutName(e.getProject(), repositoryName + "Repository");

        if(phpClass == null) {
            return null;
        }

        return new PhpType().add(phpClass);
    }

}
