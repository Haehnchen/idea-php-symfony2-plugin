package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GlobalExtensionVariableCollector implements TwigFileVariableCollector, TwigFileVariableCollector.TwigFileVariableCollectorExt {
    @Override
    public void collectVars(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {

        PhpIndex phpIndex = PhpIndex.getInstance(parameter.getProject());

        for(PhpClass phpClass : phpIndex.getAllSubclasses("\\Twig_ExtensionInterface")) {
            if(!PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile())) {
                // @TODO: signature vs getMethod faster?
                Method method = phpClass.findMethodByName("getGlobals");
                if(method != null) {
                    Collection<PhpReturn> phpReturns = PsiTreeUtil.findChildrenOfType(method, PhpReturn.class);
                    for(PhpReturn phpReturn: phpReturns) {
                        PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();
                        if(returnPsiElement instanceof ArrayCreationExpression) {
                            variables.putAll(PhpMethodVariableResolveUtil.getTypesOnArrayHash((ArrayCreationExpression) returnPsiElement));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
    }
}
