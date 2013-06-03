package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class PhpServiceAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isContainerGetCall(methodReference)) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = element.getProject().getComponent(Symfony2ProjectComponent.class);
        ServiceMap serviceMap = symfony2ProjectComponent.getServicesMap();

        String serviceName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
        if(serviceMap.getMap().containsKey(serviceName))  {
           return;
        }

        holder.createWarningAnnotation(element, "Missing Service");

    }
}