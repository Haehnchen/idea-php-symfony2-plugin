package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
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

        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (null == serviceClass) {
            return new PsiElement[]{};
        }

        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
    }

    protected PsiElement[] parameterGoToDeclaration(PsiElement psiElement, String psiParameterName) {

        String parameterName = ServiceXmlParserFactory.getInstance(psiElement.getProject(), ParameterServiceParser.class).getParameterMap().get(psiParameterName);
        if (null == parameterName) {
            // find local parameter
            Map<String, String> localParameter = YamlHelper.getLocalParameterMap(psiElement);
            if(localParameter.containsKey(psiParameterName)) {
                parameterName = localParameter.get(psiParameterName);
            }
        }

        if (null == parameterName) {
            return new PsiElement[]{};
        }

        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), parameterName);
    }

    protected List<TwigFile> templateGoto(PsiElement psiElement, String templateName) {
        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiElement.getProject());
        TwigFile twigFile = twigFilesByName.get(templateName);
        if (null == twigFile) {
            return Collections.emptyList();
        }

        return Arrays.asList(twigFile);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
