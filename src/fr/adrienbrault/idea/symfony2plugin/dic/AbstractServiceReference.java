package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractServiceReference extends PsiPolyVariantReferenceBase<PsiElement> {

    protected String serviceId;
    protected boolean usePrivateServices = true;

    public AbstractServiceReference(PsiElement psiElement) {
        super(psiElement);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> resolveResults = new ArrayList<>();

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = collector.resolve(serviceId);
        if (serviceClass == null) {
            return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
        }

        resolveResults.addAll(PhpElementsUtil.getClassInterfaceResolveResult(getElement().getProject(), serviceClass));
        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<>();

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        Collection<ContainerService> values = collector.getServices().values();

        if(!usePrivateServices) {
            values = ContainerUtil.filter(values, service -> !service.isPrivate());
        }

        results.addAll(
            ServiceCompletionProvider.getLookupElements(null, values).getLookupElements()
        );

        return results.toArray();
    }
}
