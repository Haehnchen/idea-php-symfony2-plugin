package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

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

        if (!(PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement))) {

            return new PsiElement[]{};
        }

        String psiText = PsiElementUtils.getText(psiElement);
        if(null == psiText || psiText.length() == 0) {
            return new PsiElement[]{};
        }

        if(psiText.startsWith("@") && psiText.length() > 1) {
            return serviceGoToDeclaration(psiElement, psiText.substring(1));
        }

        // match: %annotations.reader.class%
        if(psiText.length() > 3 && psiText.startsWith("%") && psiText.endsWith("%")) {
            return parameterGoToDeclaration(psiElement, psiText.substring(1, psiText.length() - 1));
        }

        if(psiText.contains(".") && psiText.length() > 1) {
            return serviceGoToDeclaration(psiElement, psiText);
        }

        if(psiText.contains("\\")) {
            return classGoToDeclaration(psiElement, psiText);
        }

        return new PsiElement[]{};
    }

    protected PsiElement[] classGoToDeclaration(PsiElement psiElement, String className) {
        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), className);
    }

    protected PsiElement[] serviceGoToDeclaration(PsiElement psiElement, String serviceId) {

        Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
        String serviceClass = symfony2ProjectComponent.getServicesMap().getMap().get(serviceId.toLowerCase());

        if (null == serviceClass) {
            return new PsiElement[]{};
        }

        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
    }

    protected PsiElement[] parameterGoToDeclaration(PsiElement psiElement, String psiParameterName) {

        Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
        if (null == symfony2ProjectComponent) {
            return new PsiElement[]{};
        }

        String parameterName = symfony2ProjectComponent.getConfigParameter().get(psiParameterName);
        if (null == parameterName) {
            return new PsiElement[]{};
        }

        return PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), parameterName);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
