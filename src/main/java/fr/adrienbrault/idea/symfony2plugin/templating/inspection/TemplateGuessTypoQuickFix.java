package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

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
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateGuessTypoQuickFix extends IntentionAndQuickFixAction {
    @NotNull
    private final String missingTemplateName;

    public TemplateGuessTypoQuickFix(@NotNull String missingTemplateName) {
        this.missingTemplateName = missingTemplateName;
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

        List<String> similarTemplateNames = findSimilarTemplateNames(project, this.missingTemplateName);
        if (similarTemplateNames.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No similar item found");
            return;
        }

        Consumer<String> templateSuggestion = null;

        PsiElement parent = elementAt.getParent();
        if (elementAt.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
            // TWIG
            templateSuggestion = selectedValue -> WriteCommandAction.runWriteCommandAction(project, "Template Suggestion", null, () -> {
                PsiElement firstFromText = TwigElementFactory.createPsiElement(project, "{% foo '" + selectedValue + "' }%", TwigTokenTypes.STRING_TEXT);
                elementAt.replace(firstFromText);
            });
        } else if (parent instanceof StringLiteralExpression) {
            // PHP + DocTag
            templateSuggestion = selectedValue -> WriteCommandAction.runWriteCommandAction(project, "Template Suggestion", null, () -> {
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

        if (templateSuggestion == null) {
            IdeHelper.showErrorHintIfAvailable(editor, "No replacement provider found");
            return;
        }

        if (similarTemplateNames.size() == 1 || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            templateSuggestion.consume(similarTemplateNames.get(0));
            return;
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(similarTemplateNames)
            .setTitle("Symfony: Template Suggestions")
            .setItemChosenCallback(templateSuggestion)
            .createPopup()
            .showInBestPositionFor(editor);
    }

    @NotNull
    private static List<String> findSimilarTemplateNames(@NotNull Project project, String templateNameIfMissing) {
        String strippedInput = stripTemplateFormatAndExtensionLowered(templateNameIfMissing);

        Map<String, String> strippedToOriginal = new HashMap<>();
        Set<String> strippedNames = new HashSet<>();

        for (String template : TwigUtil.getTemplateMap(project, true).keySet()) {
            String stripped = stripTemplateFormatAndExtensionLowered(template);
            strippedToOriginal.put(stripped, template);
            strippedNames.add(stripped);
        }

        return SimilarSuggestionUtil.findSimilarString(strippedInput, strippedNames).stream()
            .map(strippedToOriginal::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @NotNull
    private static String stripTemplateFormatAndExtensionLowered(@NotNull String templateNameIfMissing) {
        return templateNameIfMissing
            .toLowerCase()
            .replaceAll("\\.[^.]*$", "").replaceAll("\\.[^.]*$", "");
    }
}