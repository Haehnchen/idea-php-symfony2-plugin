package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ThreeState;
import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

public class PhpAttributeCompletionPopupHandlerCompletionConfidence {
    /**
     * Tells IntelliJ that completion should definitely run after "#" in PHP classes
     * This is needed for auto-popup to work for PHP attributes
     *
     * @author Daniel Espendiller <daniel@espendiller.net>
     */
    public static class PhpAttributeCompletionConfidence extends CompletionConfidence {
        @NotNull
        @Override
        public ThreeState shouldSkipAutopopup(@NotNull Editor editor, @NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
            if (offset <= 0 || !(psiFile instanceof PhpFile)) {
                return ThreeState.UNSURE;
            }

            Project project = editor.getProject();
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return ThreeState.UNSURE;
            }

            // Check if there's a "#" before the cursor in the document
            CharSequence documentText = editor.getDocument().getCharsSequence();
            if (documentText.charAt(offset - 1) == '#' && psiFile.findElementAt(offset - 2) instanceof PsiWhiteSpace) {
                // Check if we should provide attribute completions for this context
                // (controller class, twig component, twig extension, etc.)
                if (PhpAttributeScopeValidator.shouldProvideAttributeCompletions(contextElement, project)) {
                    return ThreeState.NO;
                }
            }

            return ThreeState.UNSURE;
        }
    }

    /**
     * Triggers auto-popup completion after typing '#' character in PHP files
     * when positioned before a public method or class (for PHP attributes like #[Route()])
     *
     * @author Daniel Espendiller <daniel@espendiller.net>
     */
    public static class PhpAttributeAutoPopupHandler extends TypedHandlerDelegate {
        public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            if (charTyped != '#' || !(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
                return Result.CONTINUE;
            }


            // Check if we're in a class context
            int offset = editor.getCaretModel().getOffset();
            if (!(file.findElementAt(offset - 2) instanceof PsiWhiteSpace) && !(file.findElementAt(offset - 1) instanceof PsiWhiteSpace)) {
                return Result.CONTINUE;
            }

            PsiElement element = file.findElementAt(offset - 1);
            if (element == null) {
                return Result.CONTINUE;
            }

            // Check if we should provide attribute completions for this context
            // (controller class, twig component, twig extension, etc.)
            if (!PhpAttributeScopeValidator.shouldProvideAttributeCompletions(element, project)) {
                return Result.CONTINUE;
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
    }
}
