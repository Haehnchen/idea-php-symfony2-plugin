package de.espend.idea.php.drupal.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.utils.DrupalPattern;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!DrupalProjectComponent.isEnabled(psiElement)) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElementList = new ArrayList<>();

        PsiElement context = psiElement.getContext();
        if(context instanceof StringLiteralExpression && DrupalPattern.isAfterArrayKey(psiElement, "route_name")) {
            PsiElement routeTarget = RouteHelper.getRouteNameTarget(psiElement.getProject(), ((StringLiteralExpression) context).getContents());
            if(routeTarget != null) {
                psiElementList.add(routeTarget);
            }
        }

        return psiElementList.toArray(new PsiElement[psiElementList.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
