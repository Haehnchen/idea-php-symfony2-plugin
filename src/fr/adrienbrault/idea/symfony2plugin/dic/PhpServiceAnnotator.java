package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;


public class PhpServiceAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject()) || !Settings.getInstance(element.getProject()).phpAnnotateService) {
            return;
        }

        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isContainerGetCall(methodReference)) {
            return;
        }

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(element.getProject(), XmlServiceParser.class).getServiceMap();

        String serviceName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
        if(serviceMap.getMap().containsKey(serviceName))  {
           return;
        }

        holder.createWarningAnnotation(element, "Missing Service");

    }
}