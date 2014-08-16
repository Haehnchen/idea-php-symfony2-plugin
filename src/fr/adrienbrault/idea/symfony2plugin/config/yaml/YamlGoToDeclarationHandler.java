package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

        // only string values like "foo", foo
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
            psiElements.addAll(Arrays.asList((parameterGoToDeclaration(psiElement, psiText))));
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

        serviceId = YamlHelper.trimSpecialSyntaxServiceName(serviceId).toLowerCase();

        String serviceClass = ContainerCollectionResolver.resolveService(psiElement.getProject(), serviceId);

        if (serviceClass != null) {
            PsiElement[] targetElements = PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
            if(targetElements.length > 0) {
                return targetElements;
            }
        }

        // get container target on indexes
        List<PsiElement> possibleServiceTargets = ServiceIndexUtil.findServiceDefinitions(psiElement.getProject(), serviceId);
        return possibleServiceTargets.toArray(new PsiElement[possibleServiceTargets.size()]);

    }

    protected PsiElement[] parameterGoToDeclaration(PsiElement psiElement, String psiParameterName) {

        if(!YamlHelper.isValidParameterName(psiParameterName)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> targets = ServiceUtil.getServiceClassTargets(psiElement.getProject(), psiParameterName);
        return targets.toArray(new PsiElement[targets.size()]);
    }

    protected List<PsiFile> templateGoto(PsiElement psiElement, String templateName) {
        return Arrays.asList(TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName));
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
