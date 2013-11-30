package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String serviceId;
    private boolean useIndexedServices = false;

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);
        serviceId = element.getContents();
    }

    public ServiceReference(@NotNull StringLiteralExpression element, boolean useIndexedServices) {
        this(element);
        this.useIndexedServices = useIndexedServices;
   }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();

        if(this.useIndexedServices) {
            ServiceIndexUtil.attachIndexServiceResolveResults(getElement().getProject(), serviceId.toLowerCase(), resolveResults);
        }

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (null != serviceClass) {
            resolveResults.addAll(PhpElementsUtil.getClassInterfaceResolveResult(this.getElement().getProject(), serviceClass));
        }

        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        Set<String> knownServices = new HashSet<String>();

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        List<LookupElement> results = new ArrayList<LookupElement>();
        for(Map.Entry<String, String> entry: serviceMap.getPublicMap().entrySet()) {
            String serviceId = entry.getKey();
            String serviceClass = entry.getValue();
            Collection<PhpClass> phpClasses = phpIndex.getAnyByFQN(serviceClass);
            if (phpClasses.size() > 0) {
                knownServices.add(serviceId);
                results.add(new ServiceLookupElement(serviceId, phpClasses.iterator().next()));
            }
        }

        if(this.useIndexedServices) {
            for(String serviceName: ServiceIndexUtil.getAllServiceNames(getElement().getProject())) {
                if(!knownServices.contains(serviceName)) {
                    results.add(new ServiceStringLookupElement(serviceName));
                }
            }
        }

        return results.toArray();
    }
}
