package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ServiceLineMarkerProvider extends RelatedItemLineMarkerProvider {


    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof StringLiteralExpression) || !(psiElement.getContext() instanceof ParameterList)) {
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

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE).
            setTargets(resolveResults).
            setTooltipText("Navigate to service");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

}

