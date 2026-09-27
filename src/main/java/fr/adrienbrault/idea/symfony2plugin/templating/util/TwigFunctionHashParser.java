package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Locates a caret inside a hash literal passed to a Twig function, e.g. <code>{{ button({ size: '<caret>' }) }}</code>.
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public final class TwigFunctionHashParser {
    private TwigFunctionHashParser() {
    }

    /**
     * Resolves the hash context of a key or a string value.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{{ button({ <caret> }) }}</code> is a key of argument {@code 0}</li>
     *   <li><code>{{ button('Save', { icon: { '<caret>' } }) }}</code> is a quoted key below {@code icon} of argument {@code 1}</li>
     *   <li><code>{{ button({ size: '<caret>' }) }}</code> is the value of {@code size}</li>
     * </ul>
     */
    @Nullable
    public static FunctionHash parse(@NotNull PsiElement element) {
        boolean isString = isTwigElementType(element, TwigTokenTypes.STRING_TEXT);
        if (!isString && !isTwigElementType(element, TwigTokenTypes.IDENTIFIER)) {
            return null;
        }

        PsiElement token = element;
        PsiElement parent = element.getParent();
        if (parent != null && isTwigElementType(parent, TwigElementTypes.VARIABLE_REFERENCE)) {
            token = parent;
        }

        PsiElement hashLiteral = token.getParent();
        if (hashLiteral == null || !isHashLiteral(hashLiteral)) {
            return null;
        }

        PsiElement previous = previousMeaningfulSibling(token);
        if (isString) {
            if (previous == null || !isQuote(previous)) {
                return null;
            }

            previous = previousMeaningfulSibling(previous);
        }

        if (previous == null) {
            return null;
        }

        String valueKey = null;
        if (isTwigElementType(previous, TwigTokenTypes.COLON)) {
            valueKey = isString ? readKeyBefore(previous) : null;
            if (valueKey == null) {
                return null;
            }
        } else if (!isEntryStart(previous)) {
            return null;
        }

        List<String> keyPath = new ArrayList<>();
        PsiElement current = hashLiteral;
        while (current.getParent() != null && isHashLiteral(current.getParent())) {
            PsiElement colon = previousMeaningfulSibling(current);
            String key = colon != null && isTwigElementType(colon, TwigTokenTypes.COLON) ? readKeyBefore(colon) : null;
            if (key == null) {
                return null;
            }

            keyPath.add(key);
            current = current.getParent();
        }

        PsiElement functionCall = current.getParent();
        if (functionCall == null || !isTwigElementType(functionCall, TwigElementTypes.FUNCTION_CALL)) {
            return null;
        }

        PsiElement functionName = PsiElementUtils.getChildrenOfType(functionCall, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
        Integer argumentIndex = getArgumentIndex(functionCall, current);
        if (functionName == null || argumentIndex == null) {
            return null;
        }

        Collections.reverse(keyPath);

        return new FunctionHash(functionName.getText(), argumentIndex, keyPath, valueKey, isString);
    }

    /**
     * Counts the positional arguments before the hash, e.g. {@code 1} for <code>button('Save', {})</code>.
     */
    @Nullable
    private static Integer getArgumentIndex(@NotNull PsiElement functionCall, @NotNull PsiElement hashLiteral) {
        // named arguments like "options: {}" have no positional index
        PsiElement previous = previousMeaningfulSibling(hashLiteral);
        if (previous == null || !isArgumentStart(previous)) {
            return null;
        }

        int index = 0;
        boolean insideArguments = false;
        for (PsiElement child = functionCall.getFirstChild(); child != null && child != hashLiteral; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigTokenTypes.LBRACE)) {
                insideArguments = true;
            } else if (insideArguments && isTwigElementType(child, TwigTokenTypes.COMMA)) {
                index++;
            }
        }

        return insideArguments ? index : null;
    }

    /**
     * Reads the key in front of a colon, e.g. {@code size} for {@code size:}, {@code 'size':} or {@code "size":}.
     */
    @Nullable
    private static String readKeyBefore(@NotNull PsiElement colon) {
        PsiElement key = previousMeaningfulSibling(colon);
        if (key == null) {
            return null;
        }

        if (isTwigElementType(key, TwigTokenTypes.IDENTIFIER) || isTwigElementType(key, TwigElementTypes.VARIABLE_REFERENCE)) {
            return key.getText();
        }

        if (!isQuote(key)) {
            return null;
        }

        PsiElement text = previousMeaningfulSibling(key);
        if (text == null || !isTwigElementType(text, TwigTokenTypes.STRING_TEXT)) {
            return null;
        }

        PsiElement openingQuote = previousMeaningfulSibling(text);
        boolean isClosed = openingQuote != null && openingQuote.getNode().getElementType() == key.getNode().getElementType();

        return isClosed ? text.getText() : null;
    }

    private static boolean isHashLiteral(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigElementTypes.LITERAL) &&
            element.getFirstChild() != null &&
            isTwigElementType(element.getFirstChild(), TwigTokenTypes.LBRACE_CURL);
    }

    private static boolean isEntryStart(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigTokenTypes.LBRACE_CURL) || isTwigElementType(element, TwigTokenTypes.COMMA);
    }

    private static boolean isArgumentStart(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigTokenTypes.LBRACE) || isTwigElementType(element, TwigTokenTypes.COMMA);
    }

    private static boolean isQuote(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigTokenTypes.SINGLE_QUOTE) || isTwigElementType(element, TwigTokenTypes.DOUBLE_QUOTE);
    }

    @Nullable
    private static PsiElement previousMeaningfulSibling(@NotNull PsiElement element) {
        PsiElement previous = element.getPrevSibling();
        while (previous != null && (previous instanceof PsiWhiteSpace || isTwigElementType(previous, TwigTokenTypes.WHITE_SPACE))) {
            previous = previous.getPrevSibling();
        }

        return previous;
    }

    private static boolean isTwigElementType(@NotNull PsiElement element, @NotNull IElementType elementType) {
        ASTNode node = element.getNode();
        return node != null && node.getElementType() == elementType;
    }

    /**
     * Hash context of a Twig function argument.
     *
     * @param functionName Twig function name, e.g. {@code button}
     * @param argumentIndex positional index of the top-level hash inside the function call
     * @param keyPath keys from the top-level hash down to the hash holding the caret, e.g. {@code [icon]}
     * @param valueKey key whose string value holds the caret, or {@code null} when the caret is on a key
     * @param quoted whether the caret is inside quotes
     */
    public record FunctionHash(
        @NotNull String functionName,
        int argumentIndex,
        @NotNull List<String> keyPath,
        @Nullable String valueKey,
        boolean quoted
    ) {
    }
}
