package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String classFQN;
    private boolean provideVariants = false;

    private boolean useClasses = true;
    private boolean useInterfaces = false;

    public PhpClassReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.classFQN = PsiElementUtils.getMethodParameter(element);
    }

    public PhpClassReference(@NotNull PsiElement element, String classFQN) {
        super(element);
        this.classFQN = classFQN;
    }

    public PhpClassReference(@NotNull PsiElement element, String classFQN, boolean provideVariants) {
        this(element, classFQN);
        this.provideVariants = provideVariants;
    }

    public PhpClassReference(@NotNull StringLiteralExpression element, boolean provideVariants) {
        this(element);
        this.provideVariants = provideVariants;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> results = new ArrayList<>();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        if(this.useClasses) {
            this.attachPhpClassResolveResults(phpIndex.getClassesByFQN(classFQN), results);
        }

        if(this.useInterfaces) {
            this.attachPhpClassResolveResults(phpIndex.getInterfacesByFQN(classFQN.startsWith("\\") ? classFQN : "\\" + classFQN), results);
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    private void attachPhpClassResolveResults(Collection<PhpClass> phpClasses, List<ResolveResult> results) {
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<>();

        // not break old calls to this
        // @TODO: check performance and migrate all custom completion to this method
        if(!this.provideVariants) {
            return results.toArray();
        }


        CamelHumpMatcher camelHumpMatcher = new CamelHumpMatcher(this.classFQN);
        PhpIndex phpIndex = PhpIndex.getInstance(this.getElement().getProject());

        if(this.useClasses) {
            for (String name : phpIndex.getAllClassNames(camelHumpMatcher)) {
                attachPhpClassToVariants(results, phpIndex.getClassesByName(name));
            }
        }

        // phpindex interface dont support filter
        if(this.useInterfaces) {
            for (String name : phpIndex.getAllInterfaceNames()) {

                if(this.classFQN.length() > 0 && !name.toLowerCase().contains(this.classFQN.toLowerCase())) {
                    continue;
                }

                attachPhpClassToVariants(results, phpIndex.getInterfacesByName(name));

            }
        }

        return results.toArray();
    }

    private void attachPhpClassToVariants(List<LookupElement> results, Collection<PhpClass> phpClasses) {
        for(PhpClass phpClass: phpClasses) {
            results.add(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
        }
    }

    public PhpClassReference setUseInterfaces(boolean useInterfaces) {
        this.useInterfaces = useInterfaces;
        return this;
    }

    public PhpClassReference setUseClasses(boolean useClasses) {
        this.useClasses = useClasses;
        return this;
    }

}
