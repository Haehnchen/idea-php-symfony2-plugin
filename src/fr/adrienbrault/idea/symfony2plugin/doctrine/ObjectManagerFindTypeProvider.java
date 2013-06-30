package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectManagerFindTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0180';

    @Override
    public char getKey() {
        return '\u0153';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).objectManagerFindTypeProvider) {
            return null;
        }

        if(!(e instanceof MethodReference) || !PhpElementsUtil.isMethodWithFirstString(e, "find")) {
            return null;
        }


        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }


        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = ((MethodReference)e).getParameters();
        if (parameters.length == 2) {
            PsiElement parameter = parameters[0];
            if ((parameter instanceof StringLiteralExpression)) {
                String param = ((StringLiteralExpression)parameter).getContents();
                if (StringUtil.isNotEmpty(param)) {
                    return refSignature + TRIM_KEY + param;
                }
            }
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // get back our original call
        String originalSignature = expression.substring(0, expression.lastIndexOf(TRIM_KEY));
        String parameter = expression.substring(expression.lastIndexOf(TRIM_KEY) + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return Arrays.asList(phpNamedElement);
        }

        if (!new Symfony2InterfacesUtil().isCallTo((Method) phpNamedElement, "\\Doctrine\\Common\\Persistence\\ObjectManager", "find")) {
            return Arrays.asList(phpNamedElement);
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if(phpClass == null) {
            return Arrays.asList(phpNamedElement);
        }

        return Arrays.asList(phpClass);

    }

}
