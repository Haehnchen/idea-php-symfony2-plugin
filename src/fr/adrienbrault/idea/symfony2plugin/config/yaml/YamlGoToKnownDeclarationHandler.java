package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

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

        if(YamlElementPatternHelper.getSingleLineScalarKey("resource").accepts(psiElement)) {
            this.getResourceGoto(psiElement, results);
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    private void getResourceGoto(PsiElement psiElement, List<PsiElement> results) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());

        if(!text.startsWith("@") || !text.contains("/")) {
            return;
        }

        String bundleName = text.substring(1, text.indexOf("/"));

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(PhpIndex.getInstance(psiElement.getProject())).getBundle(bundleName);

        if(symfonyBundle == null) {
            return;
        }

        String path = text.substring(text.indexOf("/") + 1);
        PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(psiElement.getProject(), symfonyBundle.getRelative(path));
        if(psiFile == null) {
            return;
        }

        results.add(psiFile);
    }

    private void getControllerGoto(PsiElement psiElement, List<PsiElement> results) {

        String text = PsiElementUtils.trimQuote(psiElement.getText());

        ControllerIndex controllerIndex = new ControllerIndex(PhpIndex.getInstance(psiElement.getProject()));
        ControllerAction controllerAction = controllerIndex.getControllerAction(text);

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
