package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GlobalExtensionVariableCollector implements TwigFileVariableCollector {
    private static final Key<CachedValue<Map<String, PsiVariable>>> TWIG_EXTENSION_GLOBAL_VARIABLES = new Key<>("TWIG_EXTENSION_GLOBAL_VARIABLES");

    @Override
    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        variables.putAll(getGlobals(parameter.getProject()));
    }

    @NotNull
    private static Map<String, PsiVariable> getGlobals(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TWIG_EXTENSION_GLOBAL_VARIABLES,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableMap(getGlobalsInner(project)),
                PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
            ),
            false
        );
    }

    @NotNull
    private static Map<String, PsiVariable> getGlobalsInner(@NotNull Project project) {
        Map<String, PsiVariable> variables = new HashMap<>();

        for(PhpClass phpClass : TwigUtil.getTwigExtensionClasses(project)) {
            if(!PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile())) {
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

        return variables;
    }
}
