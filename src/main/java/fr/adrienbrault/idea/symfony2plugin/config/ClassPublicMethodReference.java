package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassPublicMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String className;
    private final Project project;

    public ClassPublicMethodReference(@NotNull PsiElement element, @NotNull String className) {
        super(element);
        this.className = className;
        this.project = element.getProject();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, this.className);
        if(phpClass == null) {
            return new ResolveResult[0];
        }

        Method targetMethod = phpClass.findMethodByName(PsiElementUtils.trimQuote(getElement().getText()));
        if(targetMethod == null) {
            return new ResolveResult[0];
        }

        return PsiElementResolveResult.createResults(targetMethod);
    }

    @NotNull
    @Override
    @Deprecated
    public Object[] getVariants() {

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, this.className);
        if(phpClass == null) {
            return new Object[0];
        }

        List<LookupElement> lookupElements = new ArrayList<>();
        for(Method publicMethod: PhpElementsUtil.getClassPublicMethod(phpClass)) {
            lookupElements.add(new PhpLookupElement(publicMethod));
        }

        return lookupElements.toArray();
    }
}
