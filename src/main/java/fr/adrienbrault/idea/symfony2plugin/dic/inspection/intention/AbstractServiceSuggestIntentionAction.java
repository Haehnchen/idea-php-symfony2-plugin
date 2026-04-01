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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractServiceSuggestIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
    @NotNull
    protected final String expectedClass;

    protected AbstractServiceSuggestIntentionAction(@NotNull String expectedClass, @NotNull PsiElement psiElement) {
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

    @NotNull
    protected abstract ServiceSuggestDialog.Callback createInsertCallback(@NotNull Editor editor, @NotNull PsiElement psiElement);

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
            createInsertCallback(editor, psiElement)
        );
    }
}
