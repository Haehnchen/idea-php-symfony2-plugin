package fr.adrienbrault.idea.symfony2plugin.config;


import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceIndexUtil;
import org.jetbrains.annotations.Nullable;

public class PhpClassServiceGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
            return new PsiElement[0];
        }

        return ServiceIndexUtil.getPossibleServiceTargets((PhpClass) psiElement.getContext());
    }

    @Nullable
    @Override
    public String getActionText(DataContext context) {
        return null;
    }

}
