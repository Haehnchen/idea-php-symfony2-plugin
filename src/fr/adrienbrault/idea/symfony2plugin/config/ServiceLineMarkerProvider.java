package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ServiceLineMarkerProvider extends RelatedItemLineMarkerProvider {


    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        if(PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
            this.classNameMarker(psiElement, result);
        }

        if(psiElement instanceof StringLiteralExpression && PhpElementsUtil.getMethodReturnPattern().accepts(psiElement)) {
            this.formNameMarker(psiElement, result);
        }

        if (!Settings.getInstance(psiElement.getProject()).phpHighlightServices
            || !(psiElement instanceof StringLiteralExpression)
            || !(psiElement.getContext() instanceof ParameterList))
        {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
            return;
        }

        MethodReference method = (MethodReference) parameterList.getContext();
        if(!new Symfony2InterfacesUtil().isContainerGetCall(method)) {
            return;
        }

        String serviceId = ((StringLiteralExpression) psiElement).getContents();

        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (null == serviceClass) {
            return;
        }

        PsiElement[] resolveResults = PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
        if(resolveResults.length == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE_LINE_MARKER).
            setTargets(resolveResults).
            setTooltipText("Navigate to service");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

    private void classNameMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement[] psiElements = ServiceIndexUtil.getPossibleServiceTargets((PhpClass) psiElement.getContext());
        if(psiElements.length == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE_LINE_MARKER).
            setTargets(psiElements).
            setTooltipText("Navigate to definition");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

    private void formNameMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {
        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);

        if(method == null) {
            return;
        }

        if(new Symfony2InterfacesUtil().isCallTo(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "getParent")) {
            PsiElement psiElement1 = FormUtil.getFormTypeToClass(psiElement.getProject(), ((StringLiteralExpression) psiElement).getContents());
            if(psiElement1 != null) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER).
                    setTargets(psiElement1).
                    setTooltipText("Navigate to form type");

                result.add(builder.createLineMarkerInfo(psiElement));
            }

        }

    }

}

