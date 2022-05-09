package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
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

    public AbstractServiceReference(PsiElement psiElement, TextRange range) {
        super(psiElement, range);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        var definitions = ServiceIndexUtil.findServiceDefinitions(
            getElement().getProject(),
            serviceId
        );

        // Return the PsiElement for the service definition corresponding to the serviceId
        var results = new ArrayList<ResolveResult>();
        for (var definition : definitions) {
            results.add(new PsiElementResolveResult(definition));
        }

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = collector.resolve(serviceId);
        if (serviceClass != null) {
            var classes = PsiElementResolveResult.createResults(PhpIndex.getInstance(getElement().getProject()).getAnyByFQN(serviceClass));
            results.addAll(Arrays.asList(classes));
        }

        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        Collection<ContainerService> values = collector.getServices().values();

        if(!usePrivateServices) {
            values = ContainerUtil.filter(values, service -> !service.isPrivate());
        }

        List<LookupElement> results = new ArrayList<>(ServiceCompletionProvider.getLookupElements(null, values).getLookupElements());
        return results.toArray();
    }
}
