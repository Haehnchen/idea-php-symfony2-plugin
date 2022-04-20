package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementFactory;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteGuessTypoQuickFix extends IntentionAndQuickFixAction {
    private final String missingRoute;

    public RouteGuessTypoQuickFix(@NotNull String missingRoute) {
        this.missingRoute = missingRoute;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Symfony: Apply Similar Suggestion";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @Override
    public void applyFix(@NotNull Project project, PsiFile psiFile, @Nullable Editor editor) {
        if (editor == null) {
            return;
        }

        PsiElement elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        List<String> similarItems = SimilarSuggestionUtil.findSimilarString(this.missingRoute, RouteHelper.getAllRoutes(project).keySet());
        if (similarItems.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "No similar item found");
            return;
        }

        Consumer<String> suggestionSelected = null;

        PsiElement parent = elementAt.getParent();
        if (elementAt.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
            // TWIG
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, "Route Suggestion", null, () -> {
                PsiElement firstFromText = TwigElementFactory.createPsiElement(project, "{% foo '" + selectedValue + "' }%", TwigTokenTypes.STRING_TEXT);
                elementAt.replace(firstFromText);
            });
        } else if (parent instanceof StringLiteralExpression) {
            // PHP + DocTag
            suggestionSelected = selectedValue -> {
                WriteCommandAction.runWriteCommandAction(project, "Route Suggestion", null, () -> {
                    String contents = parent.getText();
                    String wrap = "'";
                    if (contents.length() > 0) {
                        String wrap2 = contents.substring(0, 1);
                        if (wrap2.equals("\"") || wrap2.equals("'")) {
                            wrap = wrap2;
                        }
                    }

                    StringLiteralExpression firstFromText = PhpPsiElementFactory.createFirstFromText(project, StringLiteralExpression.class, wrap + selectedValue + wrap);
                    parent.replace(firstFromText);
                });
            };
        }

        if (suggestionSelected == null) {
            HintManager.getInstance().showErrorHint(editor, "No replacement provider found");
            return;
        }

        if (similarItems.size() == 1 || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            suggestionSelected.consume(similarItems.get(0));
            return;
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(similarItems)
            .setTitle("Symfony: Route Suggestions")
            .setItemChosenCallback(suggestionSelected)
            .createPopup()
            .showInBestPositionFor(editor);
    }
}