package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses Twig include context modifiers like {@code with}, {@code only}, and {@code with_context}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TwigIncludeContextParser {
    /**
     * Utility class.
     */
    private TwigIncludeContextParser() {
    }

    /**
     * Resolves tag-style include context, e.g. <code>{% include 'card.html.twig' with {product: item} only %}</code>.
     */
    @NotNull
    public static IncludeContext resolveTagIncludeContext(@NotNull PsiElement tag) {
        return new IncludeContext(collectTagIncludeArguments(tag), !hasOnlyKeyword(tag));
    }

    /**
     * Resolves function-style include context, e.g. <code>{{ include('card.html.twig', {product: item}, with_context: false) }}</code>.
     */
    @NotNull
    public static IncludeContext resolveFunctionIncludeContext(@NotNull PsiElement templateName) {
        PsiElement functionCall = PsiElementUtils.getParentOfType(templateName, TwigElementTypes.FUNCTION_CALL);
        if (functionCall == null) {
            return new IncludeContext(Collections.emptyList(), true);
        }

        Collection<IncludeArgument> includeArguments = new ArrayList<>();
        boolean withParentContext = true;
        List<List<PsiElement>> arguments = splitFunctionArguments(functionCall);

        for (int i = 1; i < arguments.size(); i++) {
            List<PsiElement> argument = arguments.get(i);

            if (isWithContextFalseArgument(argument)) {
                withParentContext = false;
                continue;
            }

            PsiElement hashLiteral = findHashLiteral(argument);
            if (hashLiteral != null) {
                includeArguments.addAll(collectHashArguments(hashLiteral));
            }
        }

        return new IncludeContext(includeArguments, withParentContext);
    }

    /**
     * Detects isolated tag includes, e.g. <code>{% include 'card.html.twig' only %}</code>.
     */
    private static boolean hasOnlyKeyword(@NotNull PsiElement tag) {
        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigTokenTypes.ONLY_KEYWORD) || "only".equals(child.getText())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads tag include arguments, e.g. <code>with {product: item}</code>.
     */
    @NotNull
    private static Collection<IncludeArgument> collectTagIncludeArguments(@NotNull PsiElement tag) {
        PsiElement withElement = null;
        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigTokenTypes.WITH_KEYWORD) || "with".equals(child.getText())) {
                withElement = child;
                break;
            }
        }

        if (withElement == null) {
            return Collections.emptyList();
        }

        PsiElement hashLiteral = nextHashLiteral(withElement);
        return hashLiteral != null ? collectHashArguments(hashLiteral) : Collections.emptyList();
    }

    /**
     * Splits only direct include() arguments; hash literals stay whole PSI nodes.
     * Example: <code>include('card.html.twig', {product: item}, with_context: false)</code>.
     */
    @NotNull
    private static List<List<PsiElement>> splitFunctionArguments(@NotNull PsiElement functionCall) {
        List<List<PsiElement>> arguments = new ArrayList<>();
        List<PsiElement> current = new ArrayList<>();
        boolean insideArguments = false;

        for (PsiElement child = functionCall.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isWhitespace(child)) {
                continue;
            }

            if (!insideArguments) {
                // Ignore the function name and start after the opening parenthesis.
                if (isTwigElementType(child, TwigTokenTypes.LBRACE)) {
                    insideArguments = true;
                }

                continue;
            }

            if (isTwigElementType(child, TwigTokenTypes.RBRACE)) {
                if (!current.isEmpty()) {
                    arguments.add(current);
                }

                break;
            }

            if (isTwigElementType(child, TwigTokenTypes.COMMA)) {
                arguments.add(current);
                current = new ArrayList<>();
                continue;
            }

            current.add(child);
        }

        return arguments;
    }

    /**
     * Matches include context isolation, e.g. {@code with_context: false}.
     */
    private static boolean isWithContextFalseArgument(@NotNull List<PsiElement> argument) {
        if (argument.size() < 3) {
            return false;
        }

        PsiElement name = argument.get(0);
        if (!"with_context".equals(name.getText())) {
            return false;
        }

        PsiElement separator = argument.get(1);
        if (!isTwigElementType(separator, TwigTokenTypes.COLON) && !isTwigElementType(separator, TwigTokenTypes.EQ)) {
            return false;
        }

        return "false".equals(argument.get(2).getText());
    }

    /**
     * Finds the hash literal after {@code with}, e.g. <code>with {product: item}</code>.
     */
    @Nullable
    private static PsiElement nextHashLiteral(@NotNull PsiElement element) {
        PsiElement child = nextMeaningfulSibling(element);
        return child != null && isHashLiteral(child) ? child : null;
    }

    /**
     * Finds a hash literal inside a function argument, e.g. <code>{product: item}</code>.
     */
    @Nullable
    private static PsiElement findHashLiteral(@NotNull List<PsiElement> argument) {
        for (PsiElement element : argument) {
            if (isHashLiteral(element)) {
                return element;
            }
        }

        return null;
    }

    /**
     * Checks Twig hash literals, e.g. <code>{product: item}</code>.
     */
    private static boolean isHashLiteral(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigElementTypes.LITERAL) &&
            element.getFirstChild() != null &&
            isTwigElementType(element.getFirstChild(), TwigTokenTypes.LBRACE_CURL);
    }

    /**
     * Reads hash arguments, e.g. <code>{product: item, title: 'Example'}</code>.
     */
    @NotNull
    private static Collection<IncludeArgument> collectHashArguments(@NotNull PsiElement hashLiteral) {
        List<PsiElement> children = getMeaningfulChildren(hashLiteral);
        Collection<IncludeArgument> arguments = new ArrayList<>();

        for (int i = 0; i < children.size(); i++) {
            PsiElement child = children.get(i);

            if (isTwigElementType(child, TwigTokenTypes.LBRACE_CURL) || isTwigElementType(child, TwigTokenTypes.COMMA)) {
                continue;
            }

            KeyResult keyResult = readHashKey(children, i);
            if (keyResult == null || keyResult.nextIndex() >= children.size() || !isTwigElementType(children.get(keyResult.nextIndex()), TwigTokenTypes.COLON)) {
                continue;
            }

            int valueStart = keyResult.nextIndex() + 1;
            int valueEnd = valueStart;
            // Keep the value PSI slice so callers can interpret it for their own context.
            while (valueEnd < children.size() && !isTwigElementType(children.get(valueEnd), TwigTokenTypes.COMMA) && !isTwigElementType(children.get(valueEnd), TwigTokenTypes.RBRACE_CURL)) {
                valueEnd++;
            }

            arguments.add(new IncludeArgument(keyResult.name(), children.subList(valueStart, valueEnd)));
            i = valueEnd;
        }

        return arguments;
    }

    /**
     * Reads unquoted and quoted hash keys, e.g. {@code product}, {@code 'product'}, or {@code "product"}.
     */
    @Nullable
    private static KeyResult readHashKey(@NotNull List<PsiElement> children, int index) {
        PsiElement child = children.get(index);

        if (isTwigElementType(child, TwigTokenTypes.IDENTIFIER) || isTwigElementType(child, TwigTokenTypes.STRING_TEXT)) {
            return new KeyResult(child.getText(), index + 1);
        }

        if (isTwigElementType(child, TwigElementTypes.VARIABLE_REFERENCE)) {
            return new KeyResult(child.getText(), index + 1);
        }

        if (isTwigElementType(child, TwigTokenTypes.SINGLE_QUOTE) || isTwigElementType(child, TwigTokenTypes.DOUBLE_QUOTE)) {
            if (index + 2 < children.size() && isTwigElementType(children.get(index + 1), TwigTokenTypes.STRING_TEXT)) {
                PsiElement closingQuote = children.get(index + 2);
                if (closingQuote.getNode().getElementType() == child.getNode().getElementType()) {
                    return new KeyResult(children.get(index + 1).getText(), index + 3);
                }
            }
        }

        return null;
    }

    /**
     * Returns direct non-whitespace children, e.g. for predictable hash token scanning.
     */
    @NotNull
    private static List<PsiElement> getMeaningfulChildren(@NotNull PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        for (PsiElement child = element.getFirstChild(); child != null; child = nextMeaningfulSibling(child)) {
            if (!isWhitespace(child)) {
                children.add(child);
            }
        }

        return children;
    }

    /**
     * Gets the next sibling while skipping PSI and Twig whitespace.
     */
    @Nullable
    private static PsiElement nextMeaningfulSibling(@NotNull PsiElement element) {
        PsiElement next = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        while (next != null && isWhitespace(next)) {
            next = PhpPsiUtil.getNextSiblingIgnoreWhitespace(next, true);
        }

        return next;
    }

    /**
     * Handles both IntelliJ whitespace PSI and Twig whitespace tokens.
     */
    private static boolean isWhitespace(@NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace || isTwigElementType(element, TwigTokenTypes.WHITE_SPACE);
    }

    /**
     * Compares direct Twig element types, e.g. {@code TwigTokenTypes.COMMA}.
     */
    private static boolean isTwigElementType(@NotNull PsiElement element, @NotNull IElementType elementType) {
        return element.getNode().getElementType() == elementType;
    }

    /**
     * Include argument with its raw value PSI, e.g. {@code product: item.product}.
     */
    public record IncludeArgument(@NotNull String name, @NotNull List<PsiElement> valueElements) {
    }

    /**
     * Parsed include context, e.g. arguments plus whether parent variables are inherited.
     */
    public record IncludeContext(@NotNull Collection<IncludeArgument> arguments, boolean withParentContext) {
        /**
         * Returns explicit child variable names, e.g. {@code product} from <code>{product: item}</code>.
         */
        @NotNull
        public Set<String> argumentNames() {
            return arguments.stream()
                .map(IncludeArgument::name)
                .collect(Collectors.toSet());
        }
    }

    /**
     * Parsed hash key and next token index, e.g. after {@code 'product'}.
     */
    private record KeyResult(@NotNull String name, int nextIndex) {
    }
}
