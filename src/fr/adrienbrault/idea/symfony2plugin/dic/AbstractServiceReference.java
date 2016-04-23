package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractServiceReference extends PsiPolyVariantReferenceBase<PsiElement> {

    protected String serviceId;
    protected boolean useIndexedServices = false;
    protected boolean usePrivateServices = true;

    public AbstractServiceReference(PsiElement psiElement) {
        super(psiElement);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();

        ContainerCollectionResolver.ServiceCollector collector = new ContainerCollectionResolver.ServiceCollector(getElement().getProject());
        collector.addCollectorSource(ContainerCollectionResolver.Source.COMPILER);

        if(this.useIndexedServices) {
            collector.addCollectorSource(ContainerCollectionResolver.Source.INDEX);
        }

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

        List<LookupElement> results = new ArrayList<LookupElement>();

        ContainerCollectionResolver.ServiceCollector collector = new ContainerCollectionResolver.ServiceCollector(getElement().getProject());
        collector.addCollectorSource(ContainerCollectionResolver.Source.COMPILER);

        if(this.useIndexedServices) {
            collector.addCollectorSource(ContainerCollectionResolver.Source.INDEX);
        }

        // @TODO: remove getVariants
        // just simulate scope for completion
        PsiElement psiElement = ContainerUtil.find(this.getElement().getChildren(), new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                return psiElement.getNode().getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;
            }
        });

        results.addAll(
            ServiceCompletionProvider.getLookupElements(psiElement, collector.getServices().values()).getLookupElements()
        );

        return results.toArray();
    }
}
