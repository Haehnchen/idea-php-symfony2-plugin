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
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
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

            // Check if we're before a method, class, or field
            if (getMethod(contextElement) == null && getPhpClass(contextElement) == null && getField(contextElement) == null) {
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

            // Check if we're before a method, class, or field
            if (getMethod(element) == null && getField(element) == null && getPhpClass(element) == null) {
                return Result.CONTINUE;
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
    }

    /**
     * Finds a public method associated with the given element.
     * Returns the method if the element is a child of a method or if the next sibling is a method.
     *
     * @param element The PSI element to check
     * @return The public method if found, null otherwise
     */
    public static @Nullable Method getMethod(@NotNull PsiElement element) {
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
     * Finds a PhpClass associated with the given element.
     * Returns the class if the element is a child of a class or if the next sibling is a class.
     * Also handles cases where we're in the middle of an attribute list.
     *
     * @param element The PSI element to check
     * @return The PhpClass if found, null otherwise
     */
    public static @Nullable PhpClass getPhpClass(@NotNull PsiElement element) {
        if (element.getParent() instanceof PhpClass phpClass) {
            return phpClass;
        }

        // with use statement given
        PsiElement nextSiblingIgnoreWhitespace = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSiblingIgnoreWhitespace instanceof PhpClass phpClass) {
            return phpClass;
        }

        // no use statements
        if (nextSiblingIgnoreWhitespace != null && nextSiblingIgnoreWhitespace.getNode().getElementType() == PhpElementTypes.NON_LAZY_GROUP_STATEMENT) {
            if (nextSiblingIgnoreWhitespace.getFirstChild() instanceof PhpClass phpClass) {
                return phpClass;
            }
        }

        return null;
    }

    /**
     * Finds a Field (property) associated with the given element.
     * Returns the field if the element is a child of a field or if the next sibling is a field.
     *
     * @param element The PSI element to check
     * @return The Field if found, null otherwise
     */
    public static @Nullable Field getField(@NotNull PsiElement element) {
        PsiElement nextSiblingIgnoreWhitespace = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSiblingIgnoreWhitespace instanceof PhpModifierList phpModifierList && phpModifierList.hasPublic()) {
            if (phpModifierList.getNextPsiSibling() instanceof Field field) {
                return field;
            }
        }

        if (nextSiblingIgnoreWhitespace instanceof PhpPsiElement phpPsiElement) {
            PhpPsiElement firstPsiChild = phpPsiElement.getFirstPsiChild();
            if (firstPsiChild instanceof PhpModifierList phpModifierList && phpModifierList.hasPublic()) {
                PhpPsiElement nextPsiSibling = phpModifierList.getNextPsiSibling();

                if (nextPsiSibling instanceof Field field) {
                    return field;
                } else if(nextPsiSibling instanceof PhpFieldType phpFieldType) {
                    PhpPsiElement nextPsiSibling1 = phpFieldType.getNextPsiSibling();
                    if (nextPsiSibling1 instanceof Field field1) {
                        return field1;
                    }
                }
            }
        }

        return null;
    }
}
