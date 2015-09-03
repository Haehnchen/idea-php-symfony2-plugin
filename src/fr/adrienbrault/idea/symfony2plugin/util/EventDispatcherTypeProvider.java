package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0197';

    @Override
    public char getKey() {
        return '\u0187';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).symfonyContainerTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if(!(e instanceof MethodReference) || !"dispatch".equals(((MethodReference) e).getName())) {
            return null;
        }

        PsiElement[] parameters = ((MethodReference) e).getParameters();
        if(parameters.length < 2 || !(parameters[1] instanceof NewExpression)) {
            return null;
        }

        String refSignature = ((MethodReference) e).getSignature();
        if(StringUtils.isBlank(refSignature)) {
            return null;
        }

        ClassReference classReference = ((NewExpression) parameters[1]).getClassReference();
        if(classReference == null) {
            return null;
        }

        String signature = classReference.getFQN();
        if(StringUtils.isBlank(signature)) {
            return null;
        }

        return refSignature + TRIM_KEY + signature;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // get back our original call
        // since phpstorm 7.1.2 we need to validate this
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        if(!parameter.startsWith("\\")) {
            return Collections.emptySet();
        }

        PhpClass phpClass = PhpElementsUtil.getClass(project, parameter);
        if(phpClass == null) {
            return Collections.emptySet();
        }

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return phpNamedElementCollections;
        }

        PhpClass containingClass = ((Method) phpNamedElement).getContainingClass();
        if(containingClass == null) {
            return phpNamedElementCollections;
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return phpNamedElementCollections;
        }

        // finally search the classes
        if(!new Symfony2InterfacesUtil().isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface")) {
            return phpNamedElementCollections;
        }

        return Collections.singletonList(phpClass);
    }

}
