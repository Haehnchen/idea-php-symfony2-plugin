package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Provides completion for Symfony PHP attributes like #[Route()]
 *
 * Triggers when typing "#<caret>" before a public method
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeCompletionContributor extends CompletionContributor {

    private static final String ROUTE_ATTRIBUTE_FQN = "\\Symfony\\Component\\Routing\\Attribute\\Route";
    private static final String IS_GRANTED_ATTRIBUTE_FQN = "\\Symfony\\Component\\Security\\Http\\Attribute\\IsGranted";
    private static final String CACHE_ATTRIBUTE_FQN = "\\Symfony\\Component\\HttpKernel\\Attribute\\Cache";
    private static final String GROUPS_ATTRIBUTE_FQN = "\\Symfony\\Component\\Serializer\\Attribute\\Groups";
    private static final String SERIALIZED_NAME_ATTRIBUTE_FQN = "\\Symfony\\Component\\Serializer\\Attribute\\SerializedName";

    public PhpAttributeCompletionContributor() {
        // Match any element in PHP files - we'll do more specific checking in the provider
        // Using a broad pattern to catch completion after "#" character
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withLanguage(PhpLanguage.INSTANCE)),
            new PhpAttributeCompletionProvider()
        );
    }

    private static class PhpAttributeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext context,
            @NotNull CompletionResultSet result
        ) {
            PsiElement position = parameters.getPosition();
            Project project = position.getProject();

            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            // Check if we're in a context where an attribute makes sense
            if (!isAttributeContext(position)) {
                return;
            }

            // Determine the target context: method, class, or property
            AttributeContext attributeContext = determineAttributeContext(position);
            if (attributeContext == null) {
                return;
            }

            // Add completions based on context
            switch (attributeContext.type) {
                case METHOD:
                    addMethodAttributeCompletions(project, result);
                    break;
                case CLASS:
                    addClassAttributeCompletions(project, result);
                    break;
                case PROPERTY:
                    addPropertyAttributeCompletions(project, result);
                    break;
            }

            // Stop here - don't show other completions when typing "#" for attributes
            result.stopHere();
        }

        /**
         * Check if we're in a context where a PHP attribute can be added
         * This can be: before a class, before a method, or before a property
         */
        private boolean isAttributeContext(@NotNull PsiElement position) {
            // Check if position text contains or starts with "#"
            String positionText = position.getText();
            if (positionText != null && positionText.contains("#")) {
                // Verify there's whitespace or start of line before "#"
                if (isHashPrecededByWhitespace(position)) {
                    return true;
                }
            }

            // Get text before cursor - check previous leaf
            PsiElement prevLeaf = PsiTreeUtil.prevLeaf(position);
            if (prevLeaf != null) {
                String prevText = prevLeaf.getText();
                // Check if previous element is exactly "#" (not "##" or part of another token)
                if (prevText != null && prevText.equals("#")) {
                    // Check if there's whitespace before the "#"
                    PsiElement prevPrevLeaf = PsiTreeUtil.prevLeaf(prevLeaf);
                    if (prevPrevLeaf != null) {
                        String prevPrevText = prevPrevLeaf.getText();
                        if (prevPrevText != null && (prevPrevText.trim().isEmpty() || prevPrevText.contains("\n"))) {
                            return true;
                        }
                    } else {
                        // No previous element, so "#" is at the start
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Check if the "#" character is preceded by whitespace, tab, or start of line
         */
        private boolean isHashPrecededByWhitespace(@NotNull PsiElement position) {
            String text = position.getText();
            if (text == null) {
                return false;
            }

            int hashIndex = text.indexOf('#');
            if (hashIndex < 0) {
                return false;
            }

            // If "#" is at the start of the element, check previous sibling
            if (hashIndex == 0) {
                PsiElement prevLeaf = PsiTreeUtil.prevLeaf(position);
                if (prevLeaf == null) {
                    return true; // Start of file
                }
                String prevText = prevLeaf.getText();
                // Previous element should be whitespace (contains newline, space, or tab)
                return prevText != null && (prevText.trim().isEmpty() || prevText.contains("\n"));
            }

            // Check character before "#" in the same element
            char charBeforeHash = text.charAt(hashIndex - 1);
            return charBeforeHash == ' ' || charBeforeHash == '\t' || charBeforeHash == '\n';
        }

        /**
         * Context type for attribute placement
         */
        private enum AttributeContextType {
            METHOD, CLASS, PROPERTY
        }

        /**
         * Represents the context where an attribute should be placed
         */
        private static class AttributeContext {
            AttributeContextType type;
            PsiElement targetElement;

            AttributeContext(AttributeContextType type, PsiElement targetElement) {
                this.type = type;
                this.targetElement = targetElement;
            }
        }

        /**
         * Determine what kind of element the attribute will be attached to
         * Combines sibling-based detection for classes with iteration-based detection for properties/methods
         */
        private AttributeContext determineAttributeContext(@NotNull PsiElement position) {
            // Check for class (before the class keyword) - use sibling-based detection
            PhpClass foundClass = getClassAfterPosition(position);
            if (foundClass != null) {
                return new AttributeContext(AttributeContextType.CLASS, foundClass);
            }

            // For properties and methods, we need to search within the containing class
            PhpClass phpClass = PsiTreeUtil.getParentOfType(position, PhpClass.class);
            if (phpClass != null) {
                int offset = position.getTextOffset();

                // Check for property first
                for (Field field : phpClass.getOwnFields()) {
                    if (field.getTextOffset() > offset) {
                        return new AttributeContext(AttributeContextType.PROPERTY, field);
                    }
                }

                // Then check for method
                for (Method method : phpClass.getOwnMethods()) {
                    if (method.getAccess().isPublic() && method.getTextOffset() > offset) {
                        return new AttributeContext(AttributeContextType.METHOD, method);
                    }
                }
            }

            return null;
        }

        /**
         * Get class if element is before a class definition
         * Similar to PhpAttributeCompletionPopupHandlerCompletionConfidence.getClass()
         */
        private PhpClass getClassAfterPosition(@NotNull PsiElement element) {
            if (element.getParent() instanceof PhpClass phpClass) {
                return phpClass;
            }

            PsiElement nextSibling = com.jetbrains.php.lang.psi.PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
            if (nextSibling instanceof PhpClass phpClass) {
                return phpClass;
            }

            return null;
        }



        /**
         * Add attribute completions for methods
         */
        private void addMethodAttributeCompletions(@NotNull Project project, @NotNull CompletionResultSet result) {
            // Add Route attribute completion
            if (PhpElementsUtil.getClassInterface(project, ROUTE_ATTRIBUTE_FQN) != null) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeQuotedInsertHandler(ROUTE_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(routeLookupElement);
            }

            // Add IsGranted attribute completion
            if (PhpElementsUtil.getClassInterface(project, IS_GRANTED_ATTRIBUTE_FQN) != null) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeQuotedInsertHandler(IS_GRANTED_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(isGrantedLookupElement);
            }

            // Add Cache attribute completion
            if (PhpElementsUtil.getClassInterface(project, CACHE_ATTRIBUTE_FQN) != null) {
                LookupElement cacheLookupElement = LookupElementBuilder
                    .create("#[Cache]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(CACHE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new CacheAttributeInsertHandler(CACHE_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(cacheLookupElement);
            }
        }

        /**
         * Add attribute completions for classes
         */
        private void addClassAttributeCompletions(@NotNull Project project, @NotNull CompletionResultSet result) {
            // Add Route attribute completion (for controller prefix)
            if (PhpElementsUtil.getClassInterface(project, ROUTE_ATTRIBUTE_FQN) != null) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeQuotedInsertHandler(ROUTE_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(routeLookupElement);
            }

            // Add IsGranted attribute completion (for class-level authorization)
            if (PhpElementsUtil.getClassInterface(project, IS_GRANTED_ATTRIBUTE_FQN) != null) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeQuotedInsertHandler(IS_GRANTED_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(isGrantedLookupElement);
            }
        }

        /**
         * Add attribute completions for properties
         */
        private void addPropertyAttributeCompletions(@NotNull Project project, @NotNull CompletionResultSet result) {
            // Add Groups attribute completion (for serialization)
            if (PhpElementsUtil.getClassInterface(project, GROUPS_ATTRIBUTE_FQN) != null) {
                LookupElement groupsLookupElement = LookupElementBuilder
                    .create("#[Groups]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(GROUPS_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeArrayInsertHandler(GROUPS_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(groupsLookupElement);
            }

            // Add SerializedName attribute completion
            if (PhpElementsUtil.getClassInterface(project, SERIALIZED_NAME_ATTRIBUTE_FQN) != null) {
                LookupElement serializedNameLookupElement = LookupElementBuilder
                    .create("#[SerializedName]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(SERIALIZED_NAME_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeQuotedInsertHandler(SERIALIZED_NAME_ATTRIBUTE_FQN))
                    .bold();

                result.addElement(serializedNameLookupElement);
            }
        }
    }

    /**
     * Base insert handler that adds a PHP attribute
     */
    private abstract static class BasePhpAttributeInsertHandler implements InsertHandler<LookupElement> {
        protected final String attributeFqn;

        public BasePhpAttributeInsertHandler(String attributeFqn) {
            this.attributeFqn = attributeFqn;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Editor editor = context.getEditor();
            Document document = editor.getDocument();
            Project project = context.getProject();

            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();

            // Store the original insertion offset (where user typed "#")
            int originalInsertionOffset = startOffset;

            // Check if there's a "#" before the completion position
            // If yes, we need to delete it to avoid "##[Attribute()]"
            if (startOffset > 0) {
                CharSequence text = document.getCharsSequence();
                if (text.charAt(startOffset - 1) == '#') {
                    // Delete the "#" that was typed
                    document.deleteString(startOffset - 1, tailOffset);
                    originalInsertionOffset = startOffset - 1;
                } else {
                    // Delete just the dummy identifier
                    document.deleteString(startOffset, tailOffset);
                }
            } else {
                // Delete just the dummy identifier
                document.deleteString(startOffset, tailOffset);
            }

            // First commit to get proper PSI
            PsiDocumentManager.getInstance(project).commitDocument(document);
            PsiFile file = context.getFile();

            // Find the insertion position - look for the next element (method, property, or class)
            PsiElement elementAt = file.findElementAt(originalInsertionOffset);
            PhpClass phpClass = PsiTreeUtil.getParentOfType(elementAt, PhpClass.class);

            if (phpClass == null) {
                return; // Not in a class context
            }

            // Extract class name from FQN (get the last part after the last backslash)
            String className = attributeFqn.substring(attributeFqn.lastIndexOf('\\') + 1);

            // Store document length before adding import to calculate offset shift
            int documentLengthBeforeImport = document.getTextLength();

            // Add import if necessary - this will modify the document!
            String importedName = PhpElementsUtil.insertUseIfNecessary(phpClass, attributeFqn);
            if (importedName != null) {
                className = importedName;
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
            
            // Insert the attribute text and position cursor at the adjusted cursor position
            insertAttributeAndPositionCursor(editor, document, project, currentInsertionOffset, className);
        }

        /**
         * Subclasses implement this to define how the attribute is inserted and where the cursor is positioned
         * @param insertionOffset The offset where the attribute should be inserted (where user typed "#")
         */
        protected abstract void insertAttributeAndPositionCursor(Editor editor, Document document, Project project, int insertionOffset, String className);
    }

    /**
     * Insert handler that adds a PHP attribute with empty quoted parameter
     */
    private static class PhpAttributeQuotedInsertHandler extends BasePhpAttributeInsertHandler {
        public PhpAttributeQuotedInsertHandler(String attributeFqn) {
            super(attributeFqn);
        }

        @Override
        protected void insertAttributeAndPositionCursor(Editor editor, Document document, Project project, int insertionOffset, String className) {
            PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

            // Insert at the cursor position where user typed "#"
            String attributeText = "#[" + className + "(\"\")]\n";
            document.insertString(insertionOffset, attributeText);

            // Commit and reformat
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Reformat the added attribute
            CodeUtil.reformatAddedAttribute(project, document, insertionOffset);

            // After reformatting, position cursor inside the quotes: #[Attribute("<caret>")]
            // Use PSI structure to find the exact position
            psiDocManager.commitDocument(document);

            // Get fresh PSI and find the attribute we just added
            PsiFile finalFile = psiDocManager.getPsiFile(document);
            if (finalFile != null) {
                // Look for element INSIDE the inserted attribute (a few chars after insertion point)
                // This ensures we find an element within the attribute, not before it
                PsiElement elementInsideAttribute = finalFile.findElementAt(insertionOffset + 3);
                if (elementInsideAttribute != null) {
                    // Find the PhpAttribute element
                    com.jetbrains.php.lang.psi.elements.PhpAttribute phpAttribute =  PsiTreeUtil.getParentOfType(elementInsideAttribute, com.jetbrains.php.lang.psi.elements.PhpAttribute.class);

                    if (phpAttribute != null) {
                        // Search for opening quote within the attribute's text range
                        int attributeStart = phpAttribute.getTextRange().getStartOffset();
                        int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                        CharSequence attributeContent = document.getCharsSequence().subSequence(attributeStart, attributeEnd);

                        int openQuoteIndex = attributeContent.toString().indexOf("\"");
                        if (openQuoteIndex >= 0) {
                            // Position cursor right after the opening quote
                            int caretOffset = attributeStart + openQuoteIndex + 1;
                            editor.getCaretModel().moveToOffset(caretOffset);
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert handler that adds a PHP attribute with an array parameter
     */
    private static class PhpAttributeArrayInsertHandler extends BasePhpAttributeInsertHandler {
        public PhpAttributeArrayInsertHandler(String attributeFqn) {
            super(attributeFqn);
        }

        @Override
        protected void insertAttributeAndPositionCursor(Editor editor, Document document, Project project, int insertionOffset, String className) {
            PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

            // Insert at the cursor position where user typed "#"
            String attributeText = "#[" + className + "([])]\n";
            document.insertString(insertionOffset, attributeText);

            // Commit and reformat
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Reformat the added attribute
            CodeUtil.reformatAddedAttribute(project, document, insertionOffset);

            // After reformatting, position cursor inside the array: #[Groups([<caret>])]
            // Use PSI structure to find the exact position
            psiDocManager.commitDocument(document);

            // Get fresh PSI and find the attribute we just added
            PsiFile finalFile = psiDocManager.getPsiFile(document);
            if (finalFile != null) {
                // Look for element INSIDE the inserted attribute (a few chars after insertion point)
                // This ensures we find an element within the attribute, not before it
                PsiElement elementInsideAttribute = finalFile.findElementAt(insertionOffset + 3);
                if (elementInsideAttribute != null) {
                    // Find the PhpAttribute element
                    com.jetbrains.php.lang.psi.elements.PhpAttribute phpAttribute =  PsiTreeUtil.getParentOfType(elementInsideAttribute, com.jetbrains.php.lang.psi.elements.PhpAttribute.class);

                    if (phpAttribute != null) {
                        // Search for opening bracket within the attribute's text range
                        int attributeStart = phpAttribute.getTextRange().getStartOffset();
                        int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                        CharSequence attributeContent = document.getCharsSequence().subSequence(attributeStart, attributeEnd);

                        // Find the position after "([" - we want cursor between the brackets
                        String contentStr = attributeContent.toString();
                        int openBracketIndex = contentStr.indexOf("([");
                        if (openBracketIndex >= 0) {
                            // Position cursor right after the opening bracket: #[Groups([<caret>])]
                            int caretOffset = attributeStart + openBracketIndex + 2;
                            editor.getCaretModel().moveToOffset(caretOffset);
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert handler that adds the Cache attribute with empty parentheses (no quotes)
     */
    private static class CacheAttributeInsertHandler extends BasePhpAttributeInsertHandler {
        public CacheAttributeInsertHandler(String attributeFqn) {
            super(attributeFqn);
        }

        @Override
        protected void insertAttributeAndPositionCursor(Editor editor, Document document, Project project, int insertionOffset, String className) {
            PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

            // Insert at the cursor position where user typed "#"
            String attributeText = "#[" + className + "()]\n";
            document.insertString(insertionOffset, attributeText);

            // Commit and reformat
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Reformat the added attribute
            CodeUtil.reformatAddedAttribute(project, document, insertionOffset);

            // After reformatting, position cursor inside the parentheses: #[Cache(<caret>)]
            // Use PSI structure to find the exact position
            psiDocManager.commitDocument(document);

            // Get fresh PSI and find the attribute we just added
            PsiFile finalFile = psiDocManager.getPsiFile(document);
            if (finalFile != null) {
                // Look for element INSIDE the inserted attribute (a few chars after insertion point)
                // This ensures we find an element within the attribute, not before it
                PsiElement elementInsideAttribute = finalFile.findElementAt(insertionOffset + 3);
                if (elementInsideAttribute != null) {
                    // Find the PhpAttribute element
                    com.jetbrains.php.lang.psi.elements.PhpAttribute phpAttribute =  PsiTreeUtil.getParentOfType(elementInsideAttribute, com.jetbrains.php.lang.psi.elements.PhpAttribute.class);

                    if (phpAttribute != null) {
                        // Search for opening parenthesis within the attribute's text range
                        int attributeStart = phpAttribute.getTextRange().getStartOffset();
                        int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                        CharSequence attributeContent = document.getCharsSequence().subSequence(attributeStart, attributeEnd);

                        int openParenIndex = attributeContent.toString().indexOf("(");
                        if (openParenIndex >= 0) {
                            // Position cursor right after the opening parenthesis
                            int caretOffset = attributeStart + openParenIndex + 1;
                            editor.getCaretModel().moveToOffset(caretOffset);
                        }
                    }
                }
            }
        }
    }
}
