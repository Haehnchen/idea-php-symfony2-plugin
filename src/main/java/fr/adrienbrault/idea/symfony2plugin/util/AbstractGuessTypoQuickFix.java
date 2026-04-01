package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.ProblemDescriptor;
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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractGuessTypoQuickFix extends IntentionAndQuickFixAction {

    /**
     * Short label for this suggestion type, e.g. "Route", "Template", "Translation Key".
     * Used as the WriteCommandAction name ("<label> Suggestion") and popup title ("Symfony: <label> Suggestions").
     */
    @NotNull
    protected abstract String getSuggestionLabel();

    /**
     * Returns the ranked list of candidate replacements for the missing value.
     */
    @NotNull
    protected abstract List<String> getSimilarItems(@NotNull Project project);

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
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
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

        List<String> similarItems = getSimilarItems(project);
        if (similarItems.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No similar item found");
            return;
        }

        String actionName = getSuggestionLabel() + " Suggestion";
        Consumer<String> suggestionSelected = null;

        PsiElement parent = elementAt.getParent();
        if (elementAt.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
            // TWIG
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, actionName, null, () -> {
                PsiElement firstFromText = TwigElementFactory.createPsiElement(project, "{% foo '" + selectedValue + "' }%", TwigTokenTypes.STRING_TEXT);
                elementAt.replace(firstFromText);
            });
        } else if (parent instanceof StringLiteralExpression) {
            // PHP + DocTag
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, actionName, null, () -> {
                String contents = parent.getText();
                String wrap = "'";
                if (!contents.isEmpty()) {
                    String wrap2 = contents.substring(0, 1);
                    if (wrap2.equals("\"") || wrap2.equals("'")) {
                        wrap = wrap2;
                    }
                }

                StringLiteralExpression firstFromText = PhpPsiElementFactory.createFirstFromText(project, StringLiteralExpression.class, wrap + selectedValue + wrap);
                parent.replace(firstFromText);
            });
        }

        if (suggestionSelected == null) {
            IdeHelper.showErrorHintIfAvailable(editor, "No replacement provider found");
            return;
        }

        if (similarItems.size() == 1 || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            suggestionSelected.consume(similarItems.get(0));
            return;
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(similarItems)
            .setTitle("Symfony: " + getSuggestionLabel() + " Suggestions")
            .setItemChosenCallback(suggestionSelected)
            .createPopup()
            .showInBestPositionFor(editor);
    }
}
