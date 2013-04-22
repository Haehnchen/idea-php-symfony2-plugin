package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        if(psiElement.getText().startsWith("@") && psiElement.getText().length() > 1) {
            return serviceGoToDeclaration(psiElement, psiElement.getText().substring(1));
        }

        if(psiElement.getText().contains(".") && psiElement.getText().length() > 1) {
            return serviceGoToDeclaration(psiElement, psiElement.getText());
        }

        if(psiElement.getText().contains("\\")) {
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

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
