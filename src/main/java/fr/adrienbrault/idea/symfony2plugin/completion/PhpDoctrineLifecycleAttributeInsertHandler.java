package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Insert handler for Doctrine ORM lifecycle callback attributes that also adds
 * the #[HasLifecycleCallbacks] attribute to the class if not already present.
 *
 * This handler delegates to PhpDoctrineAttributeInsertHandler for the initial
 * attribute insertion, then ensures the class-level HasLifecycleCallbacks attribute
 * is present (required for lifecycle callbacks to work in Doctrine).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record PhpDoctrineLifecycleAttributeInsertHandler(
    @NotNull String attributeFqn,
    @NotNull String shortClassName,
    @NotNull PhpDoctrineAttributeInsertHandler delegate
) implements InsertHandler<LookupElement> {

    private static final String DOCTRINE_LIFECYCLE_CLASS = "\\Doctrine\\ORM\\Mapping\\HasLifecycleCallbacks";

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        // IMPORTANT: Find the target field BEFORE modifying the document
        int startOffset = context.getStartOffset();
        PsiFile file = context.getFile();
        PsiElement originalElement = file.findElementAt(startOffset);
        if (originalElement == null) {
            return;
        }

        PhpNamedElement validAttributeScope = PhpAttributeScopeValidator.getValidAttributeScope(originalElement);

        delegate.handleInsert(context, item);

        // Determine the target context (field, method, or class) dynamically using the shared scope validator
        if (!(validAttributeScope instanceof Field) && !(validAttributeScope instanceof PhpClass) && !(validAttributeScope instanceof Method)) {
            return;
        }

        PhpClass phpClass = validAttributeScope instanceof PhpClass
            ? (PhpClass) validAttributeScope
            : Objects.requireNonNull(PsiTreeUtil.getParentOfType(validAttributeScope, PhpClass.class));

        if (!phpClass.getAttributes(DOCTRINE_LIFECYCLE_CLASS).isEmpty()) {
            return;
        }

        // Determine which alias to use (or null for direct class import)
        String importedAlias = PhpDoctrineAttributeInsertHandler.findAliasInEntitiesScope(phpClass);

        String attributeText = DOCTRINE_LIFECYCLE_CLASS.substring(DOCTRINE_LIFECYCLE_CLASS.lastIndexOf("\\") + 1);

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(validAttributeScope);
        if (scopeForUseOperator != null) {
            if (importedAlias != null) {
                PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, DOCTRINE_LIFECYCLE_CLASS, importedAlias);
                attributeText = importedAlias + "\\" + attributeText;
            } else {
                String s = PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, DOCTRINE_LIFECYCLE_CLASS);
                if (s != null) {
                    attributeText = s;
                }
            }
        }

        addAttributeToMethod(file.getProject(), phpClass, "#[" + attributeText  + "]");
    }

    private void addAttributeToMethod(@NotNull Project project, @NotNull PhpClass phpClass, @NotNull String attributeText) {
        // Insert attribute text directly before the method
        PsiFile file = phpClass.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = phpClass.getTextRange().getStartOffset();
        String fullAttributeText = attributeText + "\n";

        document.insertString(methodStartOffset, fullAttributeText);
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Reformat the added attribute with proper indentation
        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);
    }
}
