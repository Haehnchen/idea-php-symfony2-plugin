package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class PhpServiceAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        // nothing todo; no annotator enabled
        if(!Settings.getInstance(element.getProject()).phpAnnotateService && !Settings.getInstance(element.getProject()).phpAnnotateWeakService) {
            return;
        }

        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isContainerGetCall(methodReference)) {
            return;
        }

        String serviceName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
        if(serviceName == null || StringUtils.isBlank(serviceName)) {
            return;
        }

        ContainerService containerService = ContainerCollectionResolver.getService(element.getProject(), serviceName);
        if(containerService != null) {
            if(Settings.getInstance(element.getProject()).phpAnnotateWeakService && containerService.isWeak()) {
                holder.createWeakWarningAnnotation(element, "Weak Service");
            }
            return;
        }

        if(Settings.getInstance(element.getProject()).phpAnnotateService) {
            holder.createWarningAnnotation(element, "Missing Service");
        }

    }
}