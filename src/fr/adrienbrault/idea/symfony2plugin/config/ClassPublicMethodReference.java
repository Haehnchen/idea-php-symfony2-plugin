package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.lookup.LookupElement;
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

public class ClassPublicMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String className;
    private String method;

    public ClassPublicMethodReference(@NotNull PsiElement element, @NotNull String className) {
        super(element);
        this.className = className;
        this.method = PsiElementUtils.trimQuote(element.getText());
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(getElement().getProject(), this.className);
        if(phpClass == null) {
            return new ResolveResult[0];
        }

        Method targetMethod = phpClass.findMethodByName(this.method);
        if(targetMethod == null) {
            return new ResolveResult[0];
        }

        return PsiElementResolveResult.createResults(targetMethod);
    }

    @NotNull
    @Override
    @Deprecated
    public Object[] getVariants() {

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(getElement().getProject(), this.className);
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
