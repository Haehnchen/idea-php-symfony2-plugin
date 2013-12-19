package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceXmlReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String serviceId;
    private boolean useIndexedServices = false;

    public ServiceXmlReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    public ServiceXmlReference(@NotNull PsiElement element, String ServiceId, boolean useIndexedServices) {
        this(element, ServiceId);
        this.useIndexedServices = useIndexedServices;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();

        // search any indexed service
        if(this.useIndexedServices) {
            ServiceIndexUtil.attachIndexServiceResolveResults(getElement().getProject(), serviceId.toLowerCase(), resolveResults);
        }

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (null == serviceClass) {
            ServiceMap localServiceMap = XmlHelper.getLocalMissingServiceMap(getElement(), null);
            if(localServiceMap == null || !localServiceMap.getMap().containsKey(serviceId)) {
                return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
            }

            serviceClass = localServiceMap.getMap().get(serviceId);
        }

        resolveResults.addAll(PhpElementsUtil.getClassInterfaceResolveResult(getElement().getProject(), serviceClass));
        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
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

        Set<String> knownServices = new HashSet<String>();

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap();
        Map<String, String> serviceMapPublicMap = serviceMap.getPublicMap();

        List<LookupElement> results = new ArrayList<LookupElement>();
        addServiceLookupElements(serviceMapPublicMap, results, knownServices);

        ServiceMap localServiceMap = XmlHelper.getLocalMissingServiceMap(getElement(), serviceMapPublicMap);
        if(localServiceMap != null && localServiceMap.getMap().size() > 0) {
            addServiceLookupElements(localServiceMap.getMap(), results, knownServices);
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

    protected void addServiceLookupElements(Map<String, String> serviceMapPublicMap, List<LookupElement> results, Set<String> knownServices) {
        for(Map.Entry<String, String> entry: serviceMapPublicMap.entrySet()) {
            if(!knownServices.contains(entry.getKey())) {
                knownServices.add(entry.getKey());
                results.add(new ServiceStringLookupElement(entry.getKey(), entry.getValue()));
            }
        }
    }
}
