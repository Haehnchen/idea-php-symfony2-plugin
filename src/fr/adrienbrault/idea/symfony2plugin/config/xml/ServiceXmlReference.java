package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceXmlReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String serviceId;

    public ServiceXmlReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());

        if (null == serviceClass) {
            ServiceMap localServiceMap = XmlHelper.getLocalMissingServiceMap(getElement(), null);
            if(localServiceMap == null || !localServiceMap.getMap().containsKey(serviceId)) {
                return new ResolveResult[]{};
            }

            serviceClass = localServiceMap.getMap().get(serviceId);
        }

        List<ResolveResult> classInterfaceResolveResult = PhpElementsUtil.getClassInterfaceResolveResult(getElement().getProject(), serviceClass);
        return classInterfaceResolveResult.toArray(new ResolveResult[classInterfaceResolveResult.size()]);
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

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap();
        Map<String, String> serviceMapPublicMap = serviceMap.getPublicMap();

        List<LookupElement> results = new ArrayList<LookupElement>();
        addServiceLookupElements(serviceMapPublicMap, results);

        ServiceMap localServiceMap = XmlHelper.getLocalMissingServiceMap(getElement(), serviceMapPublicMap);
        if(localServiceMap != null && localServiceMap.getMap().size() > 0) {
            addServiceLookupElements(localServiceMap.getMap(), results);
        }

        return results.toArray();
    }

    protected void addServiceLookupElements(Map<String, String> serviceMapPublicMap, List<LookupElement> results) {
        for(Map.Entry<String, String> entry: serviceMapPublicMap.entrySet()) {
            results.add(new ServiceStringLookupElement(entry.getKey(), entry.getValue()));
        }
    }
}
