package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.ExtendsList;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CodeUtil {

    /**
     *
     * @param phpClass PhpClass
     * @param methodName The method
     * @return last int position of method psi element
     */
    public static int getMethodInsertPosition(@NotNull PhpClass phpClass, @NotNull String methodName) {

        // empty class
        Method[] ownMethods = phpClass.getOwnMethods();
        if(ownMethods.length == 0) {
            return phpClass.getTextRange().getEndOffset() - 1;
        }

        // collection method names and sort them, to get method matching before
        List<String> methods = new ArrayList<>();
        methods.add(methodName);
        for (Method method: ownMethods) {
            methods.add(method.getName());
        }

        Collections.sort(methods);

        // first method
        int post = methods.indexOf(methodName);
        if(post == 0) {
            return phpClass.getTextRange().getEndOffset() - 1;
        }

        // find method after we should insert method
        Method method = phpClass.findOwnMethodByName(methods.get(post - 1));
        if(method == null) {
            return -1;
        }

        return method.getTextRange().getEndOffset();
    }

    /**
     * Reformats an attribute that was added to a method.
     * This ensures proper indentation and formatting for the attribute.
     *
     * @param project The project
     * @param document The document containing the method
     * @param attributeStartOffset The offset where the attribute starts
     */
    public static void reformatAddedAttribute(@NotNull Project project, @NotNull Document document, int attributeStartOffset) {
        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        PsiFile freshFile = psiDocManager.getPsiFile(document);
        if (freshFile == null) {
            return;
        }

        PsiElement elementAtOffset = freshFile.findElementAt(attributeStartOffset);
        if (elementAtOffset == null) {
            return;
        }

        Method freshMethod = PsiTreeUtil.getParentOfType(elementAtOffset, Method.class);
        if (freshMethod == null) {
            return;
        }

        PsiElement nameIdentifier = freshMethod.getNameIdentifier();
        if (nameIdentifier == null) {
            return;
        }

        int endOffset = nameIdentifier.getTextRange().getEndOffset();

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        codeStyleManager.reformatRange(freshFile, attributeStartOffset, endOffset);
    }

    /**
     * Removes an extends clause for a specific class from a PHP class declaration.
     * If the specified class is the only parent being extended, the entire extends clause
     * (including the "extends" keyword) is removed. If there are multiple extends (in interfaces/traits),
     * only the specific class reference is removed.
     *
     * @param phpClass The PHP class to modify
     * @param parentClassFqn The fully-qualified name of the parent class to remove (with or without leading backslash)
     * @return true if the extends clause was removed, false otherwise
     */
    public static boolean removeExtendsClause(@NotNull PhpClass phpClass, @NotNull String parentClassFqn) {
        ExtendsList extendsList = phpClass.getExtendsList();
        List<ClassReference> references = extendsList.getReferenceElements();

        if (references.isEmpty()) {
            return false;
        }

        // Normalize the FQN to ensure comparison works (with leading backslash)
        String normalizedFqn = parentClassFqn.startsWith("\\") ? parentClassFqn : "\\" + parentClassFqn;

        // Find the ClassReference for the parent class to remove
        ClassReference targetRef = null;
        for (ClassReference classRef : references) {
            String refFqn = classRef.getFQN();
            if ((normalizedFqn.equals(refFqn) || parentClassFqn.equals(refFqn))) {
                targetRef = classRef;
                break;
            }
        }

        if (targetRef == null) {
            return false;
        }

        // If this is the only extends, remove the entire extends clause using Document API
        // This avoids PSI null issues by editing at the text level
        if (references.size() == 1) {
            PsiFile file = phpClass.getContainingFile();
            Document document = PsiDocumentManager.getInstance(phpClass.getProject()).getDocument(file);
            if (document == null) {
                return false;
            }

            PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(phpClass.getProject());

            // Commit any pending PSI changes and unblock the document
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Find whitespace before the extends list - we want to delete from there
            // to include any spaces between the class name and "extends"
            PsiElement startElement = extendsList;
            PsiElement prev = extendsList.getPrevSibling();

            // Skip backwards through whitespace to find the actual start
            while (prev != null && (prev.getText().trim().isEmpty() || "extends".equals(prev.getText().trim()))) {
                startElement = prev;
                prev = prev.getPrevSibling();
            }

            int startOffset = startElement.getTextRange().getStartOffset();
            int endOffset = extendsList.getTextRange().getEndOffset();

            document.deleteString(startOffset, endOffset);

            // Commit and synchronize PSI
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            return true;
        } else {
            // Multiple extends, just remove this one reference (PSI deletion is safe here)
            targetRef.delete();
            return true;
        }
    }
}
