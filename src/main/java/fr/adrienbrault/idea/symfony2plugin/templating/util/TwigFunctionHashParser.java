package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     *   <li><code>{{ button(options: { <caret> }) }}</code> is a key of the argument named {@code options}</li>
     *   <li><code>{{ cva(compoundVariants: [{ size: ['<caret>'] }]) }}</code> is a list item below {@code [[], size]}</li>
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

        PsiElement container = token.getParent();
        boolean isListItem = isString && container != null && isTwigElementType(container, TwigElementTypes.ARRAY_VALUE);
        if (container == null || (!isListItem && !isHashLiteral(container))) {
            return null;
        }

        PsiElement previous = previousMeaningfulSibling(token);
        if (isString) {
            if (previous == null || !isQuote(previous)) {
                return null;
            }

            previous = previousMeaningfulSibling(previous);
        }

        String valueKey = null;
        if (isListItem) {
            container = container.getParent();
            if (previous != null || container == null || !isTwigElementType(container, TwigElementTypes.ARRAY_LITERAL)) {
                return null;
            }

            valueKey = PhpDocArrayShapeUtil.LIST_ITEM;
        } else if (previous == null) {
            return null;
        } else if (isTwigElementType(previous, TwigTokenTypes.COLON)) {
            valueKey = isString ? readKeyBefore(previous) : null;
            if (valueKey == null) {
                return null;
            }
        } else if (!isEntryStart(previous)) {
            return null;
        }

        List<String> keyPath = new ArrayList<>();
        PsiElement current = container;
        while (current.getParent() != null) {
            PsiElement containerParent = current.getParent();
            if (isHashLiteral(containerParent)) {
                PsiElement colon = previousMeaningfulSibling(current);
                String key = colon != null && isTwigElementType(colon, TwigTokenTypes.COLON) ? readKeyBefore(colon) : null;
                if (key == null) {
                    return null;
                }

                keyPath.add(key);
                current = containerParent;
            } else if (isListItemOf(containerParent)) {
                keyPath.add(PhpDocArrayShapeUtil.LIST_ITEM);
                current = containerParent.getParent();
            } else {
                break;
            }
        }

        PsiElement argumentValue = current;
        PsiElement functionCall = argumentValue.getParent();
        if (functionCall == null || !isTwigElementType(functionCall, TwigElementTypes.FUNCTION_CALL)) {
            return null;
        }

        PsiElement functionName = PsiElementUtils.getChildrenOfType(functionCall, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
        FunctionArgument argument = ContainerUtil.find(getArguments(functionCall), candidate -> candidate.value() == argumentValue);
        if (functionName == null || argument == null) {
            return null;
        }

        Collections.reverse(keyPath);

        return new FunctionHash(functionName.getText(), argument.index(), argument.name(), keyPath, valueKey, isString, functionCall);
    }

    /**
     * Arguments of a function call, e.g. {@code 'Save'} at {@code 0} and {@code {}} named {@code options} for
     * <code>button('Save', options: {})</code>; the older <code>options = {}</code> form is named too.
     */
    @NotNull
    public static List<FunctionArgument> getArguments(@NotNull PsiElement functionCall) {
        List<FunctionArgument> arguments = new ArrayList<>();

        PsiElement child = functionCall.getFirstChild();
        while (child != null && !isTwigElementType(child, TwigTokenTypes.LBRACE)) {
            child = child.getNextSibling();
        }

        int index = 0;
        PsiElement start = child;
        while (start != null && isArgumentStart(start)) {
            PsiElement value = nextMeaningfulSibling(start);
            if (value == null || isTwigElementType(value, TwigTokenTypes.RBRACE)) {
                break;
            }

            String name = null;
            PsiElement assignment = nextMeaningfulSibling(value);
            boolean isNamed = assignment != null &&
                (isTwigElementType(value, TwigElementTypes.VARIABLE_REFERENCE) || isTwigElementType(value, TwigTokenTypes.IDENTIFIER)) &&
                (isTwigElementType(assignment, TwigTokenTypes.COLON) || isTwigElementType(assignment, TwigTokenTypes.EQ));

            if (isNamed) {
                name = value.getText();
                value = nextMeaningfulSibling(assignment);
                if (value == null) {
                    break;
                }
            }

            boolean hasValue = !isTwigElementType(value, TwigTokenTypes.COMMA) && !isTwigElementType(value, TwigTokenTypes.RBRACE);
            if (hasValue) {
                arguments.add(new FunctionArgument(index, name, value));
            }

            index++;

            start = value;
            while (start != null && !isTwigElementType(start, TwigTokenTypes.COMMA) && !isTwigElementType(start, TwigTokenTypes.RBRACE)) {
                start = start.getNextSibling();
            }
        }

        return arguments;
    }

    /**
     * Keys of a hash literal with their value, e.g. {@code size} and {@code 'icon-sm'} for <code>{ size: {}, 'icon-sm': {} }</code>.
     */
    @NotNull
    public static Map<String, PsiElement> getHashEntries(@NotNull PsiElement hashLiteral) {
        Map<String, PsiElement> entries = new LinkedHashMap<>();
        if (!isHashLiteral(hashLiteral)) {
            return entries;
        }

        for (PsiElement child = hashLiteral.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!isTwigElementType(child, TwigTokenTypes.COLON)) {
                continue;
            }

            String key = readKeyBefore(child);
            PsiElement value = nextMeaningfulSibling(child);
            if (key != null && value != null) {
                entries.putIfAbsent(key, value);
            }
        }

        return entries;
    }

    /**
     * Key elements of a hash literal by key, e.g. the identifier {@code size} and the string text {@code icon-sm}
     * for <code>{ size: {}, 'icon-sm': {} }</code>.
     */
    @NotNull
    public static Map<String, PsiElement> getHashKeys(@NotNull PsiElement hashLiteral) {
        Map<String, PsiElement> keys = new LinkedHashMap<>();
        if (!isHashLiteral(hashLiteral)) {
            return keys;
        }

        for (PsiElement child = hashLiteral.getFirstChild(); child != null; child = child.getNextSibling()) {
            PsiElement key = isTwigElementType(child, TwigTokenTypes.COLON) ? getKeyBefore(child) : null;
            if (key != null) {
                keys.putIfAbsent(key.getText(), key);
            }
        }

        return keys;
    }

    public static boolean isHashLiteral(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigElementTypes.LITERAL) &&
            element.getFirstChild() != null &&
            isTwigElementType(element.getFirstChild(), TwigTokenTypes.LBRACE_CURL);
    }

    /**
     * Reads the key in front of a colon at the start of a hash entry, e.g. {@code size} for {@code size:},
     * {@code 'size':}, {@code "size":} or {@code 1:}.
     */
    @Nullable
    private static String readKeyBefore(@NotNull PsiElement colon) {
        PsiElement key = getKeyBefore(colon);
        return key != null ? key.getText() : null;
    }

    /**
     * Same as {@link #readKeyBefore} but returns the key element: the identifier, the number or the string text.
     */
    @Nullable
    private static PsiElement getKeyBefore(@NotNull PsiElement colon) {
        PsiElement key = previousMeaningfulSibling(colon);
        if (key == null) {
            return null;
        }

        if (isTwigElementType(key, TwigTokenTypes.IDENTIFIER) || isTwigElementType(key, TwigElementTypes.VARIABLE_REFERENCE) || isTwigElementType(key, TwigTokenTypes.NUMBER)) {
            return isEntryStartBefore(key) ? key : null;
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

        return isClosed && isEntryStartBefore(openingQuote) ? text : null;
    }

    private static boolean isEntryStartBefore(@NotNull PsiElement element) {
        PsiElement previous = previousMeaningfulSibling(element);
        return previous != null && isEntryStart(previous);
    }

    /**
     * Whether an element is an item of a sequence literal, e.g. the hash of <code>[{}]</code>.
     */
    private static boolean isListItemOf(@NotNull PsiElement element) {
        PsiElement arrayLiteral = element.getParent();
        return isTwigElementType(element, TwigElementTypes.ARRAY_VALUE) &&
            arrayLiteral != null &&
            isTwigElementType(arrayLiteral, TwigElementTypes.ARRAY_LITERAL);
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
        while (previous != null && isWhiteSpace(previous)) {
            previous = previous.getPrevSibling();
        }

        return previous;
    }

    @Nullable
    private static PsiElement nextMeaningfulSibling(@NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        while (next != null && isWhiteSpace(next)) {
            next = next.getNextSibling();
        }

        return next;
    }

    private static boolean isWhiteSpace(@NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace || isTwigElementType(element, TwigTokenTypes.WHITE_SPACE);
    }

    private static boolean isTwigElementType(@NotNull PsiElement element, @NotNull IElementType elementType) {
        ASTNode node = element.getNode();
        return node != null && node.getElementType() == elementType;
    }

    /**
     * Argument of a Twig function call.
     *
     * @param index position among the arguments of the call
     * @param name argument name, e.g. {@code options} for <code>options: {}</code>, or {@code null} for a positional argument
     * @param value first element of the argument value
     */
    public record FunctionArgument(int index, @Nullable String name, @NotNull PsiElement value) {
    }

    /**
     * Hash context of a Twig function argument.
     *
     * @param functionName Twig function name, e.g. {@code button}
     * @param argumentIndex position of the top-level literal among the arguments of the call
     * @param argumentName name of the argument holding the top-level literal, or {@code null} for a positional argument
     * @param keyPath keys from the top-level literal down to the literal holding the caret, e.g. {@code [icon]};
     *                {@link PhpDocArrayShapeUtil#LIST_ITEM} for an item of a sequence literal
     * @param valueKey key whose string value holds the caret, {@link PhpDocArrayShapeUtil#LIST_ITEM} for a string
     *                 item of a sequence literal, or {@code null} when the caret is on a key
     * @param quoted whether the caret is inside quotes
     * @param functionCall the Twig function call
     */
    public record FunctionHash(
        @NotNull String functionName,
        int argumentIndex,
        @Nullable String argumentName,
        @NotNull List<String> keyPath,
        @Nullable String valueKey,
        boolean quoted,
        @NotNull PsiElement functionCall
    ) {
    }
}
