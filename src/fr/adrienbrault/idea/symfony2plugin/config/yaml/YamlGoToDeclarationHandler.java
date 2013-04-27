package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if (!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)) {
            return new PsiElement[]{};
        }
        if(psiElement == null) {
            return new PsiElement[]{};
        }

        String psiText = psiElement.getText();
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
            return classGoToDeclaration(psiElement);
        }

        return new PsiElement[]{};
    }

    protected PsiElement[] classGoToDeclaration(PsiElement psiElement) {
        String className = psiElement.getText();

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(className);

        List<PsiElement> results = new ArrayList<PsiElement>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass).getElement());
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    protected PsiElement[] serviceGoToDeclaration(PsiElement psiElement, String serviceId) {

        Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
        String serviceClass = symfony2ProjectComponent.getServicesMap().getMap().get(serviceId.toLowerCase());

        if (null == serviceClass) {
            return new PsiElement[]{};
        }

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(serviceClass);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(serviceClass);

        List<PsiElement> results = new ArrayList<PsiElement>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass).getElement());
        }

        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface).getElement());
        }

        return results.toArray(new PsiElement[results.size()]);
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

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        results.addAll(PhpElementsUtil.getClassInterfaceResolveResult(psiElement.getProject(), parameterName));

        // self add; so variable is not marked as invalid eg in xml
        if(results.size() == 0) {
            return new PsiElement[]{};
        }

        return new PsiElement[]{results.get(0).getElement()};
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
