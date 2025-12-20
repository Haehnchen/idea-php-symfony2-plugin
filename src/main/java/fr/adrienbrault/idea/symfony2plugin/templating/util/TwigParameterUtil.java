package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for Twig parameter handling, especially named arguments
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigParameterUtil {

    /**
     * Extract filter/function name from parameter position by walking up PSI tree
     *
     * @param parameterPosition Position in the parameter list
     * @return Filter or function name, or null if not found
     */
    @Nullable
    public static String extractFunctionOrFilterName(@NotNull PsiElement parameterPosition) {
        // Walk up to find function call or filter expression
        PsiElement parent = parameterPosition.getParent();

        while (parent != null) {
            IElementType elementType = parent.getNode().getElementType();

            // Check for function call
            if (elementType == TwigElementTypes.FUNCTION_CALL ||
                parent.getNode().toString().contains("FUNCTION_CALL") ||
                parent.getNode().toString().contains("PARENTHESIZED_EXPRESSION")) {

                // Find IDENTIFIER in children or siblings
                PsiElement identifier = findIdentifierInFunctionContext(parent);
                if (identifier != null) {
                    return identifier.getText();
                }
            }

            // Check for filter by looking for FILTER token in children
            PsiElement filterToken = findChildOfType(parent, TwigTokenTypes.FILTER);
            if (filterToken != null) {
                // Find IDENTIFIER after FILTER token
                PsiElement identifier = findIdentifierAfterFilter(parent);
                if (identifier != null) {
                    return identifier.getText();
                }
            }

            parent = parent.getParent();
        }

        return null;
    }

    /**
     * Find IDENTIFIER in function context, handling PhpStorm version differences
     */
    @Nullable
    private static PsiElement findIdentifierInFunctionContext(@NotNull PsiElement functionContext) {
        // PhpStorm 2024.x: function name is outside PARENTHESIZED_EXPRESSION
        if (functionContext.getNode().toString().contains("PARENTHESIZED_EXPRESSION")) {
            PsiElement prevSibling = functionContext.getPrevSibling();
            while (prevSibling != null) {
                if (prevSibling.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                    return prevSibling;
                }
                if (!(prevSibling instanceof PsiWhiteSpace)) {
                    break;
                }
                prevSibling = prevSibling.getPrevSibling();
            }
        }

        // Earlier versions: function name is inside FUNCTION_CALL
        PsiElement identifier = PsiElementUtils.getChildrenOfType(
            functionContext,
            com.intellij.patterns.PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
        );
        if (identifier != null) {
            return identifier;
        }

        // Try first child
        PsiElement firstChild = functionContext.getFirstChild();
        if (firstChild != null && firstChild.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
            return firstChild;
        }

        return null;
    }

    /**
     * Find child element with specific token type
     */
    @Nullable
    private static PsiElement findChildOfType(@NotNull PsiElement parent, @NotNull IElementType tokenType) {
        for (PsiElement child : parent.getChildren()) {
            if (child.getNode().getElementType() == tokenType) {
                return child;
            }
            PsiElement found = findChildOfType(child, tokenType);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Find IDENTIFIER after FILTER token
     */
    @Nullable
    private static PsiElement findIdentifierAfterFilter(@NotNull PsiElement filterElement) {
        for (PsiElement child : filterElement.getChildren()) {
            if (child.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                return child;
            }
            // Recursively search in children
            PsiElement found = findIdentifierAfterFilter(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Determine if element is in a filter context vs function
     *
     * @param element Element to check
     * @return true if in filter context, false otherwise
     */
    public static boolean isFilterContext(@NotNull PsiElement element) {
        // Walk up to find FILTER token
        PsiElement parent = element.getParent();
        while (parent != null) {
            // Check if this element or its children contain a FILTER token
            if (findChildOfType(parent, TwigTokenTypes.FILTER) != null) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Get already-used parameter names in the current call
     *
     * @param callContext Element within the parameter list
     * @return Set of parameter names already used
     */
    @NotNull
    public static Set<String> getUsedParameterNames(@NotNull PsiElement callContext) {
        Set<String> usedParams = new HashSet<>();

        // Find the parameter list parent
        PsiElement parent = callContext.getParent();
        while (parent != null) {
            String nodeText = parent.getNode().toString();
            if (nodeText.contains("FUNCTION_CALL") ||
                nodeText.contains("PARENTHESIZED_EXPRESSION") ||
                findChildOfType(parent, TwigTokenTypes.FILTER) != null) {
                break;
            }
            parent = parent.getParent();
        }

        if (parent == null) {
            return usedParams;
        }

        // Find all IDENTIFIER + COLON pairs
        collectNamedParameters(parent, usedParams);

        return usedParams;
    }

    /**
     * Recursively collect named parameters (IDENTIFIER followed by COLON)
     */
    private static void collectNamedParameters(@NotNull PsiElement element, @NotNull Set<String> usedParams) {
        for (PsiElement child : element.getChildren()) {
            if (child.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                // Check if next non-whitespace sibling is COLON
                PsiElement nextSibling = child.getNextSibling();
                while (nextSibling != null && (nextSibling instanceof PsiWhiteSpace ||
                       nextSibling.getNode().getElementType() == TwigTokenTypes.WHITE_SPACE)) {
                    nextSibling = nextSibling.getNextSibling();
                }
                if (nextSibling != null && nextSibling.getNode().getElementType() == TwigTokenTypes.COLON) {
                    usedParams.add(child.getText());
                }
            }
            // Recurse into children
            collectNamedParameters(child, usedParams);
        }
    }

    /**
     * Calculate parameter offset accounting for Twig internal parameters
     * - needs_environment, needs_context (from TwigExtension options)
     * - First parameter for filters (the piped value)
     *
     * @param extension Twig extension metadata
     * @param isFilter Whether this is a filter (vs function)
     * @return Offset to skip internal parameters
     */
    public static int calculateParameterOffset(@NotNull TwigExtension extension, boolean isFilter) {
        int offset = 0;

        // Skip environment parameter if needed
        if ("true".equals(extension.getOption("needs_environment"))) {
            offset++;
        }

        // Skip context parameter if needed
        if ("true".equals(extension.getOption("needs_context"))) {
            offset++;
        }

        // For filters, skip first parameter (the piped value)
        if (isFilter) {
            offset++;
        }

        return offset;
    }
}
