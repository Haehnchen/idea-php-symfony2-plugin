package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Insert a handler that adds Doctrine ORM attributes with intelligent alias detection.
 *
 * This handler automatically determines whether to use:
 * - An existing alias (e.g., "ORM", "DoctrineORM")
 * - Plain FQN (e.g., "\Doctrine\ORM\Mapping\Column")
 *
 * The decision is based on analyzing:
 * 1. Existing patterns in the current file
 * 2. Patterns in other entities in the same namespace
 * 3. Default fallback: plain FQN
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record PhpDoctrineAttributeInsertHandler(@NotNull String attributeFqn, @NotNull String shortClassName)
    implements InsertHandler<LookupElement> {

    private static final String DOCTRINE_MAPPING_NAMESPACE = "\\Doctrine\\ORM\\Mapping";

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Editor editor = context.getEditor();
        Document document = editor.getDocument();
        Project project = context.getProject();

        int startOffset = context.getStartOffset();
        int tailOffset = context.getTailOffset();

        // IMPORTANT: Find the target field BEFORE modifying the document
        PsiFile file = context.getFile();
        PsiElement originalElement = file.findElementAt(startOffset);
        if (originalElement == null) {
            return;
        }

        // Determine the target context (field, method, or class) dynamically using shared scope validator
        PhpNamedElement validAttributeScope = PhpAttributeScopeValidator.getValidAttributeScope(originalElement);
        if (!(validAttributeScope instanceof Field) && !(validAttributeScope instanceof PhpClass) && !(validAttributeScope instanceof Method)) {
            return;
        }

        PhpClass phpClass = validAttributeScope instanceof PhpClass
            ? (PhpClass) validAttributeScope
            : Objects.requireNonNull(PsiTreeUtil.getParentOfType(validAttributeScope, PhpClass.class));

        // Find and delete the "#" before the completion position to avoid "##[Attribute()]"
        CharSequence text = document.getCharsSequence();
        int deleteStart = startOffset;

        // Check startOffset - 1 and startOffset - 2 for the "#" character
        if (startOffset > 0 && text.charAt(startOffset - 1) == '#') {
            deleteStart = startOffset - 1;
        } else if (startOffset > 1 && text.charAt(startOffset - 2) == '#') {
            // Handle case where there might be a single whitespace between # and dummy identifier
            deleteStart = startOffset - 2;
        }

        // Delete from the "#" (or startOffset if no "#" found) to tailOffset
        document.deleteString(deleteStart, tailOffset);

        // Store the original insertion offset (where user typed "#")
        int originalInsertionOffset = deleteStart;

        // Commit after deletion
        PsiDocumentManager.getInstance(project).commitDocument(document);

        // Store document length before adding import to calculate offset shift
        int documentLengthBeforeImport = document.getTextLength();

        // Determine which alias to use (or null for direct class import)
        String importedAlias;
        String ormAttributeAlias = getOrmAttributeAlias(phpClass);
        if (hasDoctrineOrmAttribute(phpClass) || ormAttributeAlias != null) {
            importedAlias = ormAttributeAlias;
        } else {
            importedAlias = findDoctrineOrmAliasInNamespaceScope(phpClass);
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(validAttributeScope);
        if (scopeForUseOperator != null) {
            if (importedAlias != null) {
                // Import namespace with alias: use Doctrine\ORM\Mapping as ORM;
                PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, DOCTRINE_MAPPING_NAMESPACE, importedAlias);
            } else {
                // Import concrete class: use Doctrine\ORM\Mapping\Column;
                PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, attributeFqn);
            }
        }

        // IMPORTANT: After adding import, commit and recalculate the insertion position
        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Calculate how much the document length changed (import adds characters above our insertion point)
        int documentLengthAfterImport = document.getTextLength();
        int offsetShift = documentLengthAfterImport - documentLengthBeforeImport;

        // Adjust insertion offset by the shift caused by import
        int currentInsertionOffset = originalInsertionOffset + offsetShift;

        // Check if there's already a newline at the current position
        CharSequence currentText = document.getCharsSequence();
        boolean hasNewlineAfter = false;
        if (currentInsertionOffset < currentText.length()) {
            char nextChar = currentText.charAt(currentInsertionOffset);
            hasNewlineAfter = (nextChar == '\n' || nextChar == '\r');
        }

        // Build attribute text - either with alias (e.g., "#[ORM\Column]") or direct class name (e.g., "#[Column]")
        String newline = hasNewlineAfter ? "" : "\n";
        String attributeText;
        if (importedAlias != null) {
            // Use namespace alias: #[ORM\Column]
            attributeText = "#[" + importedAlias + "\\" + shortClassName + "]" + newline;
        } else {
            // Use direct class name: #[Column]
            attributeText = "#[" + shortClassName + "]" + newline;
        }

        // Insert at the cursor position where user typed "#"
        document.insertString(currentInsertionOffset, attributeText);

        // Commit and reformat
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Reformat the added attribute
        CodeUtil.reformatAddedAttribute(project, document, currentInsertionOffset);

        // After reformatting, position cursor at the end of the attribute
        psiDocManager.commitDocument(document);

        // Get fresh PSI and find the attribute we just added
        PsiFile finalFile = psiDocManager.getPsiFile(document);
        if (finalFile != null) {
            // Look for element INSIDE the inserted attribute (a few chars after insertion point)
            PsiElement elementInsideAttribute = finalFile.findElementAt(currentInsertionOffset + 3);
            if (elementInsideAttribute != null) {
                // Find the PhpAttribute element
                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(elementInsideAttribute, PhpAttribute.class);

                if (phpAttribute != null) {
                    int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                    // Position cursor at the end of the line (after the closing bracket and newline)
                    editor.getCaretModel().moveToOffset(attributeEnd + 1);
                }
            }
        }
    }

    /**
     * Finds the Doctrine ORM alias used by other classes in the same namespace.
     *
     * This is a fallback when the current file has no Doctrine ORM attributes yet.
     * It looks at nearby classes (up to 3) to discover the project's alias convention.
     *
     * @return The alias to use (e.g., "ORM", "DoctrineORM"), or null for direct class import
     */
    private static String findDoctrineOrmAliasInNamespaceScope(@NotNull PhpClass currentPhpClass) {
        String fqn = currentPhpClass.getFQN();
        String namespacePrefix = fqn.substring(0, fqn.lastIndexOf("\\"));

        int h = 0;
        // Filter to only include classes with Doctrine ORM attributes
        for (PhpClass phpClass : PhpIndexUtil.getPhpClassInsideNamespace(currentPhpClass.getProject(), namespacePrefix)) {
            if (phpClass.getFQN().equals(namespacePrefix)) {
                continue;
            }

            if (h >= 3) {
                break;
            }

            h++;

            if (hasDoctrineOrmAttribute(phpClass)) {
                return getOrmAttributeAlias(phpClass);
            }
        }

        // Step 3: No pattern found - import concrete class without alias
        return null;
    }

    /**
     * Detects if Doctrine ORM attributes use an alias in the file by analyzing use statements.
     *
     * @return The alias used (e.g., "ORM", "DoctrineORM"), or null if no alias found
     */
    private static String getOrmAttributeAlias(@NotNull PhpClass phpClass) {
        // Check use statements to see if there's an alias for Doctrine\ORM\Mapping
        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(phpClass);
        if (scopeForUseOperator != null) {
            for (Map.Entry<String, String> entry : PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).entrySet()) {
                if (DOCTRINE_MAPPING_NAMESPACE.equals(entry.getValue())) {
                    return entry.getKey(); // Return the alias (e.g., "ORM", "DoctrineORM")
                }
            }
        }

        // Has Doctrine attributes but no namespace alias - return null
        return null;
    }

    /**
     * Checks if a PHP class has any Doctrine ORM attribute.
     *
     * @return true if the class has any Doctrine ORM attribute on class, fields, or methods
     */
    private static boolean hasDoctrineOrmAttribute(@NotNull PhpClass phpClass) {
        // Check class-level attributes
        if (hasDoctrineOrmAttributeInCollection(phpClass.getAttributes())) {
            return true;
        }

        // Check field attributes
        for (Field field : phpClass.getFields()) {
            if (hasDoctrineOrmAttributeInCollection(field.getAttributes())) {
                return true;
            }
        }

        // Check method attributes (for lifecycle callbacks like PrePersist, PostLoad, etc.)
        for (Method method : phpClass.getOwnMethods()) {
            if (hasDoctrineOrmAttributeInCollection(method.getAttributes())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if any attribute in the collection is a Doctrine ORM attribute.
     *
     * @return true if any attribute is from Doctrine\ORM\Mapping namespace
     */
    private static boolean hasDoctrineOrmAttributeInCollection(@NotNull Collection<PhpAttribute> attributes) {
        if (attributes.isEmpty()) {
            return false;
        }

        for (PhpAttribute attribute : attributes) {
            String fqn = attribute.getFQN();
            if (fqn != null && fqn.startsWith(DOCTRINE_MAPPING_NAMESPACE)) {
                return true;
            }
        }

        return false;
    }
}
