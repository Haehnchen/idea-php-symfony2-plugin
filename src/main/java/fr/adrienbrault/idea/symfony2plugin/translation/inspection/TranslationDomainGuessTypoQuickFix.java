package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.lookup.LookupElement;
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
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainGuessTypoQuickFix extends IntentionAndQuickFixAction {
    private final String missingTranslationDomain;

    public TranslationDomainGuessTypoQuickFix(@NotNull String missingTranslationDomain) {
        this.missingTranslationDomain = missingTranslationDomain;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Symfony: Apply Similar Suggestion";
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
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

        Set<String> domains = TranslationUtil.getTranslationDomainLookupElements(project)
            .stream()
            .map(LookupElement::getLookupString)
            .collect(Collectors.toSet());

        List<String> similarItems = SimilarSuggestionUtil.findSimilarString(this.missingTranslationDomain, domains);
        if (similarItems.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "No similar item found");
            return;
        }

        Consumer<String> suggestionSelected = null;

        PsiElement parent = elementAt.getParent();
        if (elementAt.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
            // TWIG
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, "Translation Domain Suggestion", null, () -> {
                PsiElement firstFromText = TwigElementFactory.createPsiElement(project, "{% foo '" + selectedValue + "' }%", TwigTokenTypes.STRING_TEXT);
                elementAt.replace(firstFromText);
            });
        } else if (parent instanceof StringLiteralExpression) {
            // PHP + DocTag
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, "Translation Domain Suggestion", null, () -> {
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
            .setTitle("Symfony: Translation Domain Suggestions")
            .setItemChosenCallback(suggestionSelected)
            .createPopup()
            .showInBestPositionFor(editor);
    }
}