package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider {

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).objectRepositoryResultTypeProvider) {
            return null;
        }

        // filter out method calls without parameter
        // $this->get('service_name')
        if(!PlatformPatterns
            .psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(PlatformPatterns
                .psiElement(PhpElementTypes.PARAMETER_LIST)
            ).accepts(e)) {

            return null;
        }

        MethodReference methodRef = (MethodReference) e;
        String methodRefName = methodRef.getName();
        if(null == methodRefName || !Arrays.asList(new String[] {"find", "findOneBy", "findAll", "findBy"}).contains(methodRefName)) {
            return null;
        }

        // at least one parameter is necessary on some finds
        PsiElement[] parameters = methodRef.getParameters();
        if(!methodRefName.equals("findAll")) {
            if(parameters.length == 0) {
                return null;
            }
        } else if(parameters.length != 0) {
            return null;
        }

        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
        if (!interfacesUtil.isObjectRepositoryCall(e)) {
            return null;
        }

        // @TODO: find the previously defined type instead of try it on the parameter, we now can rely on it!
        // find the called repository name on method before
        if(!(methodRef.getFirstChild() instanceof MethodReference)) {
            return null;
        }

        String repositoryName = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) methodRef.getFirstChild());
        PhpClass phpClass = EntityHelper.resolveShortcutName(e.getProject(), repositoryName);

        if(phpClass == null) {
            return null;
        }


        if(methodRefName.equals("findAll") || methodRefName.equals("findBy")) {
            return new PhpType().add(phpClass.getFQN() + "[]");
        }

        return new PhpType().add(phpClass);
    }

}
