package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.AddRouteAttributeIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides completion for Symfony PHP attributes like #[Route()] and #[AsController]
 *
 * Triggers when typing "#<caret>" before a public method or class
 *
 * Supports:
 * - Class-level attributes: #[Route], #[AsController], #[IsGranted], #[AsTwigComponent]
 * - Method-level attributes: #[Route], #[IsGranted], #[Cache]
 * - Twig extension attributes: #[AsTwigFilter], #[AsTwigFunction], #[AsTwigTest]
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeCompletionContributor extends CompletionContributor {

    private static final String ROUTE_ATTRIBUTE_FQN = "\\Symfony\\Component\\Routing\\Attribute\\Route";
    private static final String IS_GRANTED_ATTRIBUTE_FQN = "\\Symfony\\Component\\Security\\Http\\Attribute\\IsGranted";
    private static final String CACHE_ATTRIBUTE_FQN = "\\Symfony\\Component\\HttpKernel\\Attribute\\Cache";
    private static final String AS_CONTROLLER_ATTRIBUTE_FQN = "\\Symfony\\Component\\HttpKernel\\Attribute\\AsController";
    private static final String AS_TWIG_FILTER_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFilter";
    private static final String AS_TWIG_FUNCTION_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFunction";
    private static final String AS_TWIG_TEST_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigTest";
    private static final String AS_TWIG_COMPONENT_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";
    private static final String TWIG_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension";

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
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition();
            Project project = position.getProject();

            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            // Check if we're in a context where an attribute makes sense (after "#" with whitespace before it)
            if (!isAttributeContext(parameters)) {
                return;
            }

            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Check if we're before a public method (using shared logic from PhpAttributeCompletionPopupHandlerCompletionConfidence)
            Method method = PhpAttributeCompletionPopupHandlerCompletionConfidence.getMethod(position);
            if (method != null) {
                // Method-level attribute completions
                PhpClass containingClass = method.getContainingClass();
                if (containingClass != null && AddRouteAttributeIntention.isControllerClass(containingClass)) {
                    lookupElements.addAll(getControllerMethodCompletions(project));
                }

                if (containingClass != null && isTwigExtensionClass(containingClass)) {
                    lookupElements.addAll(getTwigExtensionCompletions(project));
                }
            } else {
                // Check if we're before a class
                PhpClass phpClass = PhpAttributeCompletionPopupHandlerCompletionConfidence.getPhpClass(position);
                if (phpClass != null) {
                    // Class-level attribute completions
                    if (AddRouteAttributeIntention.isControllerClass(phpClass)) {
                        lookupElements.addAll(getControllerClassCompletions(project));
                    }

                    if (isTwigComponentClass(project, phpClass)) {
                        lookupElements.addAll(getTwigComponentClassCompletions(project));
                    }
                }
            }

            // Stop here - don't show other completions when typing "#" for attributes
            if (!lookupElements.isEmpty()) {
                result.addAllElements(lookupElements);
                result.stopHere();
            }
        }

        /**
         * Get controller method-level attribute completions (for methods in controller classes)
         */
        private Collection<LookupElement> getControllerMethodCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Route attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_FQN)) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(ROUTE_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(routeLookupElement);
            }

            // Add IsGranted attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, IS_GRANTED_ATTRIBUTE_FQN)) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(IS_GRANTED_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(isGrantedLookupElement);
            }

            // Add Cache attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, CACHE_ATTRIBUTE_FQN)) {
                LookupElement cacheLookupElement = LookupElementBuilder
                    .create("#[Cache]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(CACHE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(CACHE_ATTRIBUTE_FQN, CursorPosition.INSIDE_PARENTHESES))
                    .bold();

                lookupElements.add(cacheLookupElement);
            }

            return lookupElements;
        }

        /**
         * Get controller class-level attribute completions (for controller classes)
         */
        private Collection<LookupElement> getControllerClassCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Route attribute completion (for class-level route prefix)
            if (PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_FQN)) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(ROUTE_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(routeLookupElement);
            }

            // Add AsController attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_CONTROLLER_ATTRIBUTE_FQN)) {
                LookupElement asControllerLookupElement = LookupElementBuilder
                    .create("#[AsController]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_CONTROLLER_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_CONTROLLER_ATTRIBUTE_FQN, CursorPosition.NONE))
                    .bold();

                lookupElements.add(asControllerLookupElement);
            }

            // Add IsGranted attribute completion (for class-level security)
            if (PhpElementsUtil.hasClassOrInterface(project, IS_GRANTED_ATTRIBUTE_FQN)) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(IS_GRANTED_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(isGrantedLookupElement);
            }

            return lookupElements;
        }

        private Collection<LookupElement> getTwigExtensionCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add AsTwigFilter attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_FILTER_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigFilter]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_FILTER_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_FILTER_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(lookupElement);
            }

            // Add AsTwigFunction attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_FUNCTION_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigFunction]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_FUNCTION_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_FUNCTION_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(lookupElement);
            }

            // Add AsTwigTest attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_TEST_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigTest]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_TEST_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_TEST_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get Twig component class-level attribute completions (for component classes)
         */
        private Collection<LookupElement> getTwigComponentClassCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add AsTwigComponent attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_COMPONENT_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigComponent]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_COMPONENT_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_COMPONENT_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
                    .bold();

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Check if the class is a Twig component class.
         * A class is considered a Twig component if:
         * - Its namespace contains "\\Components\\" or ends with "\\Components", OR
         * - There are existing component classes (from index) in the same namespace
         * (e.g., App\Twig\Components\Button, Foo\Components\Form\Input)
         */
        private boolean isTwigComponentClass(@NotNull Project project, @NotNull PhpClass phpClass) {
            String fqn = phpClass.getFQN();
            if (fqn.isBlank()) {
                return false;
            }

            fqn = StringUtils.stripStart(fqn, "\\");

            int lastBackslash = fqn.lastIndexOf('\\');
            if (lastBackslash == -1) {
                return false; // No namespace
            }

            String namespace = fqn.substring(0, lastBackslash);
            if (namespace.contains("\\Components\\") ||
                namespace.endsWith("\\Components") ||
                namespace.equals("Components")) {
                return true;
            }

            // Check if there are any component classes in the same namespace from the index
            //  keys are FQN class names of components with #[AsTwigComponent] attribute
            for (String key : IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)) {
                String componentFqn = StringUtils.stripStart(key, "\\");

                // Extract namespace from the component FQN
                int componentLastBackslash = componentFqn.lastIndexOf('\\');
                if (componentLastBackslash == -1) {
                    continue;
                }

                // Check if the current class's namespace matches the component namespace
                String componentNamespace = componentFqn.substring(0, componentLastBackslash);
                if (namespace.equals(componentNamespace)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Check if the class is a TwigExtension class.
         * A class is considered a TwigExtension if:
         * - Its name ends with "TwigExtension", OR
         * - It extends AbstractExtension or implements ExtensionInterface, OR
         * - Any other public method in the class already has an AsTwig* attribute
         */
        private boolean isTwigExtensionClass(@NotNull PhpClass phpClass) {
            // Check if the class name ends with "TwigExtension"
            if (phpClass.getName().endsWith("TwigExtension")) {
                return true;
            }

            // Check if the class extends AbstractExtension
            if (PhpElementsUtil.isInstanceOf(phpClass, TWIG_EXTENSION_FQN)) {
                return true;
            }

            // Check if any other public method in the class has an AsTwig* attribute
            for (Method ownMethod : phpClass.getOwnMethods()) {
                if (!ownMethod.getAccess().isPublic() || ownMethod.isStatic()) {
                    continue;
                }

                // Collect attributes once and check for any AsTwig* attribute
                Collection<PhpAttribute> attributes = ownMethod.getAttributes();
                for (PhpAttribute attribute : attributes) {
                    String fqn = attribute.getFQN();
                    if (AS_TWIG_FILTER_ATTRIBUTE_FQN.equals(fqn) ||
                        AS_TWIG_FUNCTION_ATTRIBUTE_FQN.equals(fqn) ||
                        AS_TWIG_TEST_ATTRIBUTE_FQN.equals(fqn)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Check if we're in a context where typing "#" for attributes makes sense
         * (i.e., after "#" character with whitespace before it)
         */
        private boolean isAttributeContext(@NotNull CompletionParameters parameters) {
            int offset = parameters.getOffset();
            PsiFile psiFile = parameters.getOriginalFile();

            // Need at least 2 characters before cursor to check for "# " pattern
            if (offset < 2) {
                return false;
            }

            // Check if there's a "#" before the cursor with whitespace before it
            // secure length check
            CharSequence documentText = parameters.getEditor().getDocument().getCharsSequence();
            if (offset < documentText.length()) {
                return documentText.charAt(offset - 1) == '#' && psiFile.findElementAt(offset - 2) instanceof PsiWhiteSpace;
            }

            return false;
        }
    }

    /**
     * Enum to specify where the cursor should be positioned after attribute insertion
     */
    private enum CursorPosition {
        /** Position cursor inside quotes: #[Attribute("<caret>")] */
        INSIDE_QUOTES,
        /** Position cursor inside parentheses: #[Attribute(<caret>)] */
        INSIDE_PARENTHESES,
        /** No parentheses needed: #[Attribute]<caret> */
        NONE
    }

    /**
     * Insert handler that adds a PHP attribute
     */
    private record PhpAttributeInsertHandler(@NotNull String attributeFqn, @NotNull CursorPosition cursorPosition) implements InsertHandler<LookupElement> {

    @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Editor editor = context.getEditor();
            Document document = editor.getDocument();
            Project project = context.getProject();

            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();

            // IMPORTANT: Find the target class/method BEFORE modifying the document
            // because PSI structure might change after deletions
            PsiFile file = context.getFile();
            PsiElement originalElement = file.findElementAt(startOffset);
            if (originalElement == null) {
                return;
            }

            // Determine the target context (method or class) dynamically
            PhpClass phpClass;
            Method targetMethod = PhpAttributeCompletionPopupHandlerCompletionConfidence.getMethod(originalElement);
            if (targetMethod != null) {
                // We're in a method context
                phpClass = targetMethod.getContainingClass();
            } else {
                // Try class context
                phpClass = PhpAttributeCompletionPopupHandlerCompletionConfidence.getPhpClass(originalElement);
                if (phpClass == null) {
                    return;
                }
            }

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

            // Commit after deletion
            PsiDocumentManager.getInstance(project).commitDocument(document);

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

            // Check if there's already a newline at the current position
            // to avoid adding double newlines
            CharSequence currentText = document.getCharsSequence();
            boolean hasNewlineAfter = false;
            if (currentInsertionOffset < currentText.length()) {
                char nextChar = currentText.charAt(currentInsertionOffset);
                hasNewlineAfter = (nextChar == '\n' || nextChar == '\r');
            }

            // Build attribute text based on cursor position
            String attributeText;
            String newline = hasNewlineAfter ? "" : "\n";

            if (cursorPosition == CursorPosition.INSIDE_QUOTES) {
                attributeText = "#[" + className + "(\"\")]" + newline;
            } else if (cursorPosition == CursorPosition.INSIDE_PARENTHESES) {
                attributeText = "#[" + className + "()]" + newline;
            } else {
                // CursorPosition.NONE - no parentheses
                attributeText = "#[" + className + "]" + newline;
            }

            // Insert at the cursor position where user typed "#"
            document.insertString(currentInsertionOffset, attributeText);

            // Commit and reformat
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Reformat the added attribute
            CodeUtil.reformatAddedAttribute(project, document, currentInsertionOffset);

            // After reformatting, position cursor based on the cursor position mode
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
                        int attributeStart = phpAttribute.getTextRange().getStartOffset();
                        int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                        CharSequence attributeContent = document.getCharsSequence().subSequence(attributeStart, attributeEnd);

                        if (cursorPosition == CursorPosition.NONE) {
                            // For attributes without parentheses, position cursor at the end of the line
                            // (after the closing bracket and newline)
                            editor.getCaretModel().moveToOffset(attributeEnd + 1);
                        } else {
                            // Find cursor position based on mode
                            String searchChar = cursorPosition == CursorPosition.INSIDE_QUOTES ? "\"" : "(";
                            int searchIndex = attributeContent.toString().indexOf(searchChar);

                            if (searchIndex >= 0) {
                                // Position cursor right after the search character
                                int caretOffset = attributeStart + searchIndex + 1;
                                editor.getCaretModel().moveToOffset(caretOffset);
                            }
                        }
                    }
                }
            }
        }
    }
}
