package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
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

        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
        if (!interfacesUtil.isObjectRepositoryCall(e)) {
            return null;
        }

        MethodReference met = (MethodReference) e;
        String methodName = met.getName();

        // at least one parameter is necessary on some finds
        if(null != methodName && !methodName.equals("findAll")) {
            PsiElement[] parameters = met.getParameters();
            if(parameters.length == 0) {
                return null;
            }
        }

        // @TODO: find the previously defined type instead of try it on the parameter, we now can rely on it!
        // find the called repository name on method before
        if(!(met.getFirstChild() instanceof MethodReference)) {
            return null;
        }

        String repositoryName = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) met.getFirstChild());
        PhpClass phpClass = EntityHelper.resolveShortcutName(e.getProject(), repositoryName);

        if(phpClass == null) {
            return null;
        }


        if(null != methodName && (methodName.equals("findAll") || methodName.equals("findBy"))) {
            return new PhpType().add(phpClass.getFQN() + "[]");
        }

        return new PhpType().add(phpClass);
    }

}
