package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String serviceId;

    public ServiceReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    public ServiceReference(@NotNull StringLiteralExpression element) {
        super(element);

        serviceId = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());

        if (null == serviceClass) {
            return new ResolveResult[]{};
        }

        List<ResolveResult> resolveResults = PhpElementsUtil.getClassInterfaceResolveResult(this.getElement().getProject(), serviceClass);
        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        List<LookupElement> results = new ArrayList<LookupElement>();
        for(Map.Entry<String, String> entry: serviceMap.getPublicMap().entrySet()) {
            String serviceId = entry.getKey();
            String serviceClass = entry.getValue();
            Collection<PhpClass> phpClasses = phpIndex.getAnyByFQN(serviceClass);
            if (phpClasses.size() > 0) {
                results.add(new ServiceLookupElement(serviceId, phpClasses.iterator().next()));
            }
        }

        return results.toArray();
    }
}
