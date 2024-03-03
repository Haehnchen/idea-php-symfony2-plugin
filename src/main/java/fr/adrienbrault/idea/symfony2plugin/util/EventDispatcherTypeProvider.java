package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherTypeProvider implements PhpTypeProvider4 {

    private static final char TRIM_KEY = '\u0197';

    @Override
    public char getKey() {
        return '\u0187';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!Settings.getInstance(e.getProject()).pluginEnabled) {
            return null;
        }

        // container calls are only on "get" methods
        if(!(e instanceof MethodReference) || !"dispatch".equals(((MethodReference) e).getName())) {
            return null;
        }

        PsiElement[] parameters = ((MethodReference) e).getParameters();
        if(parameters.length < 2) {
            return null;
        }

        String refSignature = ((MethodReference) e).getSignature();
        if(StringUtils.isBlank(refSignature)) {
            return null;
        }

        String signature = null;
        if(parameters[1] instanceof NewExpression) {
            // dispatch('foo', new FooEvent());
            ClassReference classReference = ((NewExpression) parameters[1]).getClassReference();
            if(classReference == null) {
                return null;
            }

            signature = classReference.getFQN();
            if(StringUtils.isBlank(signature)) {
                return null;
            }
        } else if(parameters[1] instanceof Variable) {
            // $event = new FooEvent();
            // dispatch('foo', $event);
            String firstVariableInstance = PhpElementsUtil.getFirstVariableTypeInScope((Variable) parameters[1]);
            if(firstVariableInstance == null) {
                return null;
            }

            signature = firstVariableInstance;
        } else {
            return null;
        }

        return new PhpType().add("#" + this.getKey() + refSignature + TRIM_KEY + signature);
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
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
        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
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
        if(!PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface")) {
            return phpNamedElementCollections;
        }

        return Collections.singletonList(phpClass);
    }

}
