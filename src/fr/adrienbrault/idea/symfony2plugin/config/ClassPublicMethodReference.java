package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ClassPublicMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String className;
    private String method;

    public ClassPublicMethodReference(@NotNull PsiElement element, String className) {
        super(element);
        this.className = className;
        this.method = PsiElementUtils.trimQuote(element.getText());
    }

    public ClassPublicMethodReference(@NotNull StringLiteralExpression element, String className) {
        super(element);
        this.className = className;
        this.method = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        String localClassName = this.className;
        if(!localClassName.startsWith("\\")) {
            localClassName = "\\" + localClassName;
        }

        for (PsiElement psiElement : PhpElementsUtil.getPsiElementsBySignature(getElement().getProject(), "#M#C" + localClassName + "." + this.method)) {
            results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        PhpClass phpClass = PhpElementsUtil.getClass(getElement().getProject(), this.className);

        if(phpClass == null) {
            return new Object[0];
        }

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();
        for(Method publicMethod: PhpElementsUtil.getClassPublicMethod(phpClass)) {
            lookupElements.add(new PhpLookupElement(publicMethod));
        }

        return lookupElements.toArray();
    }
}
