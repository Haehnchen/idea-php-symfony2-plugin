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
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            if (offset <= 0 || !(psiFile instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(editor.getProject())) {
                return ThreeState.UNSURE;
            }

            // Check if we're in a valid attribute context (before method, class, or property)
            if (!isValidAttributeContext(contextElement)) {
                return ThreeState.UNSURE;
            }

            // Check if there's a "#" before the cursor in the document
            CharSequence documentText = editor.getDocument().getCharsSequence();
            if (documentText.charAt(offset - 1) == '#' && psiFile.findElementAt(offset - 2) instanceof PsiWhiteSpace) {
                return ThreeState.NO;
            }

            return ThreeState.UNSURE;
        }
    }

    /**
     * Triggers auto-popup completion after typing '#' character in PHP files
     * when positioned before a public method, class, or property (for PHP attributes like #[Route()])
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
            if (!(file.findElementAt(offset - 2) instanceof PsiWhiteSpace)) {
                return Result.CONTINUE;
            }

            PsiElement element = file.findElementAt(offset - 1);
            if (element == null) {
                return Result.CONTINUE;
            }

            // Check if we're in a valid attribute context (before method, class, or property)
            if (!isValidAttributeContext(element)) {
                return Result.CONTINUE;
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
    }

    /**
     * Check if we're in a valid context for attribute completion
     * Valid contexts are: before a method, before a class, or before a property
     */
    private static boolean isValidAttributeContext(@NotNull PsiElement element) {
        // Check for method
        Method foundMethod = getMethod(element);
        if (foundMethod != null) {
            return true;
        }

        // Check for property
        Field foundField = getField(element);
        if (foundField != null) {
            return true;
        }

        // Check for class
        PhpClass foundClass = getClass(element);
        if (foundClass != null) {
            return true;
        }

        return false;
    }

    /**
     * Get method if element is before a public method
     */
    private static @Nullable Method getMethod(@NotNull PsiElement element) {
        Method foundMethod = null;

        if (element.getParent() instanceof Method method) {
            foundMethod = method;
        } else if (PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true) instanceof Method method) {
            foundMethod = method;
        }

        return foundMethod != null && foundMethod.getAccess().isPublic()
            ? foundMethod
            : null;
    }

    /**
     * Get field if element is before a property
     */
    private static @Nullable Field getField(@NotNull PsiElement element) {
        if (element.getParent() instanceof Field field) {
            return field;
        }

        PsiElement nextSibling = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSibling instanceof Field field) {
            return field;
        }

        return null;
    }

    /**
     * Get class if element is before a class definition
     */
    private static @Nullable PhpClass getClass(@NotNull PsiElement element) {
        if (element.getParent() instanceof PhpClass phpClass) {
            return phpClass;
        }

        PsiElement nextSibling = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSibling instanceof PhpClass phpClass) {
            return phpClass;
        }

        return null;
    }
}
