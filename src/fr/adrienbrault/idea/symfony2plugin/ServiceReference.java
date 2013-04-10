package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String serviceId;

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);

        serviceId = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        // Return the PsiElement for the class corresponding to the serviceId
        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        if (null == symfony2ProjectComponent) {
            return new ResolveResult[]{};
        }

        String serviceClass = symfony2ProjectComponent.getServicesMap().getMap().get(serviceId);

        if (null == serviceClass) {
            return new ResolveResult[]{};
        }

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(serviceClass);

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
        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);

        return symfony2ProjectComponent.getServicesMap().getPublicMap().keySet().toArray();
    }
}
