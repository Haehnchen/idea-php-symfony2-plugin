package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String classFQN;
    private boolean provideVariants = false;

    public PhpClassReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.classFQN = PsiElementUtils.getMethodParameter(element);
    }

    public PhpClassReference(@NotNull PsiElement element, String classFQN) {
        super(element);
        this.classFQN = classFQN;
    }

    public PhpClassReference(@NotNull StringLiteralExpression element, boolean provideVariants) {
        this(element);
        this.provideVariants = provideVariants;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(classFQN);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<LookupElement>();

        // not break old calls to this
        // @TODO: check performance and migrate all custom completion to this method
        if(!this.provideVariants) {
            return results.toArray();
        }

        PhpIndex phpIndex = PhpIndex.getInstance(this.getElement().getProject());
        for (String name : phpIndex.getAllClassNames(new CamelHumpMatcher(this.classFQN))) {
            Collection<PhpClass> classes = phpIndex.getClassesByName(name);
            for(PhpClass phpClass: classes) {
                results.add(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
            }
        }

        return results.toArray();
    }
}
