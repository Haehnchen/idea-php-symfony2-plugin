package fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Quick-fix for {@link fr.adrienbrault.idea.symfony2plugin.dic.inspection.PhpServiceInstanceInspection} that
 * opens a service-suggestion dialog and replaces the service id inside the current argument:
 * <ul>
 *   <li>{@code service('old_id')} / {@code ref('old_id')} — replaces the full string contents with the selected id</li>
 *   <li>{@code '@old_id'} — keeps the {@code @} prefix and replaces only the id part after it</li>
 *   <li>{@code service(OldService::class)} — replaces the entire class constant with a quoted string {@code 'new_id'}</li>
 * </ul>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpServiceSuggestIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
    @NotNull
    private final String expectedClass;

    public PhpServiceSuggestIntentionAction(@NotNull String expectedClass, @NotNull PsiElement psiElement) {
        super(psiElement);
        this.expectedClass = expectedClass;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Suggest Service";
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (editor == null) {
            return;
        }

        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(this.expectedClass);
        if (anyByFQN.isEmpty()) {
            return;
        }

        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(anyByFQN.iterator().next(), ContainerCollectionResolver.getServices(project));
        if (suggestions.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No suggestion found");
            return;
        }

        ServiceSuggestDialog.create(
            editor,
            ContainerUtil.map(suggestions, ContainerService::getName),
            new MyInsertCallback(editor, psiElement)
        );
    }

    /**
     * Replaces the service id inside the string literal.
     * - For service('old_id') and ref('old_id'): replaces the full string literal contents with new_id
     * - For '@old_id': replaces only the part after '@', keeping the '@' prefix
     */
    public static class MyInsertCallback implements ServiceSuggestDialog.Callback {
        @NotNull
        private final Editor editor;
        @NotNull
        private final PsiElement psiElement;

        public MyInsertCallback(@NotNull Editor editor, @NotNull PsiElement psiElement) {
            this.editor = editor;
            this.psiElement = psiElement;
        }

        @Override
        public void insert(@NotNull String selected) {
            int startOffset = psiElement.getTextRange().getStartOffset();
            int endOffset = psiElement.getTextRange().getEndOffset();

            if (psiElement instanceof ClassConstantReference) {
                // service(OldService::class) — replace the entire class constant with a quoted string
                editor.getDocument().replaceString(startOffset, endOffset, "'" + selected + "'");
            } else if (psiElement instanceof StringLiteralExpression stringLiteral) {
                String contents = stringLiteral.getContents();
                String rawText = psiElement.getText();

                if (contents.startsWith("@")) {
                    // raw '@old_id' — keep the '@', replace only the id part
                    int atIndex = rawText.indexOf('@');
                    if (atIndex < 0) {
                        return;
                    }
                    int replaceStart = startOffset + atIndex + 1;
                    int replaceEnd = startOffset + rawText.length() - 1;
                    editor.getDocument().deleteString(replaceStart, replaceEnd);
                    editor.getDocument().insertString(replaceStart, selected);
                } else {
                    // service('old_id') or ref('old_id') — replace the full contents
                    int replaceStart = startOffset + 1;
                    int replaceEnd = replaceStart + contents.length();
                    editor.getDocument().deleteString(replaceStart, replaceEnd);
                    editor.getDocument().insertString(replaceStart, selected);
                }
            }
        }
    }
}
