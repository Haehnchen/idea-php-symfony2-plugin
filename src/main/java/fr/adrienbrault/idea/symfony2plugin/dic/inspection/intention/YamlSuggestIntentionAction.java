package fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlSuggestIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
    @NotNull
    private final String expectedClass;

    public YamlSuggestIntentionAction(@NotNull String expectedClass, @NotNull PsiElement psiElement) {
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
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if(editor == null) {
            return;
        }

        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(this.expectedClass);
        if(anyByFQN.size() == 0) {
            return;
        }

        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(anyByFQN.iterator().next(), ContainerCollectionResolver.getServices(project));
        if(suggestions.size() == 0) {
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