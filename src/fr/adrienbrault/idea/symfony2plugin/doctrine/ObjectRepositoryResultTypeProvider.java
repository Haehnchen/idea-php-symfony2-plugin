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
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider {

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }

        // @TODO: if ObjectRepositoryTypeProvider overwrites the Repository we dont get called
        if (!SymfonyInterfacesHelper.isObjectRepositoryCall(e)) {
            return null;
        }

        MethodReference met = (MethodReference) e;

        // at least one parameter is necessary
        PsiElement[] parameters = met.getParameters();
        if(parameters.length == 0) {
            return null;
        }

        // find the called repository name on method before
        if(!(met.getFirstChild() instanceof MethodReference)) {
            return null;
        }

        String repositoryName = SymfonyInterfacesHelper.getFirstArgumentStringValue((MethodReference) met.getFirstChild());
        PhpClass phpClass = EntityHelper.resolveShortcutName(e.getProject(), repositoryName);

        if(phpClass == null) {
            return null;
        }

        return new PhpType().add(phpClass);
    }

}
