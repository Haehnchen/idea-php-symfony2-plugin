package fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlSuggestIntentionAction extends AbstractServiceSuggestIntentionAction {
    public YamlSuggestIntentionAction(@NotNull String expectedClass, @NotNull PsiElement psiElement) {
        super(expectedClass, psiElement);
    }

    @Override
    protected @NotNull ServiceSuggestDialog.Callback createInsertCallback(@NotNull Editor editor, @NotNull PsiElement psiElement) {
        return new MyInsertCallback(editor, psiElement);
    }

    /**
     * This class replace a service name by plain text modification.
     * This resolve every crazy yaml use case and lexer styles like:
     * <p>
     * - @, @?
     * - "@foo", '@foo', @foo
     */
    private record MyInsertCallback(@NotNull Editor editor, @NotNull PsiElement psiElement) implements ServiceSuggestDialog.Callback {
        @Override
        public void insert(@NotNull String selected) {
            String text = this.psiElement.getText();

            int i = getServiceChar(text);
            if (i < 0) {
                IdeHelper.showErrorHintIfAvailable(editor, "No valid char in text range");
                return;
            }

            String afterAtText = text.substring(i);

            // strip ending quotes
            int length = StringUtils.stripEnd(afterAtText, "'\"").length();

            int startOffset = this.psiElement.getTextRange().getStartOffset();
            int afterAt = startOffset + i + 1;

            editor.getDocument().deleteString(afterAt, afterAt + length - 1);
            editor.getDocument().insertString(afterAt, selected);
        }

        private int getServiceChar(@NotNull String text) {
            int i = text.lastIndexOf("@?");
            if (i >= 0) {
                return i + 1;
            }

            return text.lastIndexOf("@");
        }
    }
}
