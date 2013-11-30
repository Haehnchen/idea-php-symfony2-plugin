package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        if (!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)) {

            return new PsiElement[]{};
        }

        String psiText = PsiElementUtils.getText(psiElement);
        if(null == psiText || psiText.length() == 0) {
            return new PsiElement[]{};
        }

        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();

        if(psiText.startsWith("@") && psiText.length() > 1) {
            psiElements.addAll(Arrays.asList((serviceGoToDeclaration(psiElement, psiText.substring(1)))));
        }

        // match: %annotations.reader.class%
        if(psiText.length() > 3 && psiText.startsWith("%") && psiText.endsWith("%")) {
            psiElements.addAll(Arrays.asList((parameterGoToDeclaration(psiElement, psiText.substring(1, psiText.length() - 1)))));
        }

        if(psiText.contains("\\")) {
            psiElements.addAll(Arrays.asList(classGoToDeclaration(psiElement, psiText))) ;
        }

        if(psiText.endsWith(".twig") || psiText.endsWith(".php")) {
            psiElements.addAll(templateGoto(psiElement, psiText));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    protected PsiElement[] classGoToDeclaration(PsiElement psiElement, String className) {
        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), className);
    }

    protected PsiElement[] serviceGoToDeclaration(PsiElement psiElement, String serviceId) {

        // yaml strict=false syntax
        if(serviceId.endsWith("=")) {
            serviceId = serviceId.substring(0, serviceId.length() -1);
        }

        // resolve service on container
        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (serviceClass != null) {
            PsiElement[] targetElements = PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
            if(targetElements.length > 0) {
                return targetElements;
            }
        }

        // get container target on index
        return ServiceIndexUtil.getPossibleServiceTargets(psiElement.getProject(), serviceId);

    }

    protected PsiElement[] parameterGoToDeclaration(PsiElement psiElement, String psiParameterName) {

        if(!YamlHelper.isValidParameterName(psiParameterName)) {
            return new PsiElement[0];
        }

        String resolvedParameter = YamlHelper.resolveParameterName(psiElement, psiParameterName);
        if(resolvedParameter == null) {
            return new PsiElement[0];
        }

        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), resolvedParameter);
    }

    protected List<PsiFile> templateGoto(PsiElement psiElement, String templateName) {
        return Arrays.asList(TwigHelper.getTemplateFilesByName(psiElement.getProject(), templateName));
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
