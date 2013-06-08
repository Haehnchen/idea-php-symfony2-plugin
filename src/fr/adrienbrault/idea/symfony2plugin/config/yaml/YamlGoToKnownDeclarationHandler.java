package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToKnownDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        List<PsiElement> results = new ArrayList<PsiElement>();

        if(YamlElementPatternHelper.getSingleLineScalarKey("_controller").accepts(psiElement)) {
            this.getControllerGoto(psiElement, results);
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    private void getControllerGoto(PsiElement psiElement, List<PsiElement> results) {

        ControllerIndex controllerIndex = new ControllerIndex(PhpIndex.getInstance(psiElement.getProject()));
        ControllerAction controllerAction = controllerIndex.getControllerAction(psiElement.getText());

        if(controllerAction != null) {
            results.add(controllerAction.getMethod());
        }
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
