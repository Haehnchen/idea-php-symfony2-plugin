package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ArrayListSet;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
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
     * Finds a top-level key inside an include/embed context hash.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{% include 'card.html.twig' with {'title': item.title} %}</code> matches {@code title}</li>
     *   <li><code>{{ include('card.html.twig', {"title": item.title}) }}</code> matches {@code title}</li>
     *   <li><code>{% embed 'card.html.twig' with {title: item.title} %}</code> matches {@code title}</li>
     * </ul>
     */
    @Nullable
    public static IncludeKeyContext findIncludeKeyContext(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        PsiElement hashLiteral = PsiTreeUtil.findFirstParent(element, true, TwigIncludeContextParser::isHashLiteral);
        if (hashLiteral == null || !isHashKey(hashLiteral, element)) {
            return null;
        }

        PsiElement tag = findTagWithHash(hashLiteral);
        if (tag != null) {
            return new IncludeKeyContext(tag, null);
        }

        PsiElement templateName = findFunctionIncludeTemplateName(hashLiteral);
        if (templateName != null) {
            return new IncludeKeyContext(templateName, templateName.getText());
        }

        return findExternalIncludeKeyContext(hashLiteral);
    }

    /**
     * Collects variable names from the target template of an include/embed {@code with} hash key.
     * <p>
     * Example: for <code>{% include 'card.html.twig' with {'': item} %}</code> and a target
     * template containing <code>{{ title }}</code>, this returns {@code title} as completion input.
     */
    @NotNull
    public static Collection<IncludeWithContextTemplateVariable> getIncludeWithContextKeyVariables(@Nullable PsiElement element) {
        IncludeKeyContext context = findIncludeKeyContext(element);
        if (context == null) {
            return Collections.emptyList();
        }

        Collection<IncludeWithContextTemplateVariable> variables = new ArrayList<>();
        for (String templateName : getIncludeWithContextTemplateNames(context)) {
            for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(context.sourceElement().getProject(), templateName)) {
                if (!(psiFile instanceof TwigFile twigFile)) {
                    continue;
                }

                Map<TwigFile, String> twigFiles = new LinkedHashMap<>();
                twigFiles.put(twigFile, templateName);
                twigFiles.putAll(TwigUtil.getExtendsTemplates(twigFile));

                for (Map.Entry<TwigFile, String> entry : twigFiles.entrySet()) {
                    TwigUtil.visitTemplateVariables(entry.getKey(), pair ->
                        variables.add(new IncludeWithContextTemplateVariable(pair.getFirst(), pair.getSecond(), entry.getValue()))
                    );
                }
            }
        }

        return variables;
    }

    /**
     * Collects target PSI elements for the current include/embed {@code with} hash key.
     * <p>
     * Example: navigating from <code>{% include 'card.html.twig' with {'tit<caret>le': item.title} %}</code>
     * returns the {@code title} usage inside {@code card.html.twig}.
     */
    @NotNull
    public static Collection<PsiElement> getIncludeWithContextKeyTargets(@Nullable PsiElement element) {
        String variableName = element == null ? null : StringUtils.trimToNull(PsiElementUtils.trimQuote(element.getText()));
        if (StringUtils.isBlank(variableName)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new ArrayList<>();
        for (IncludeWithContextTemplateVariable variable : getIncludeWithContextKeyVariables(element)) {
            if (variableName.equals(variable.name())) {
                targets.add(variable.target());
            }
        }

        return targets;
    }

    /**
     * Resolves the template names referenced by a matched include/embed {@code with} hash context.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{{ include('card.html.twig', {'title': item.title}) }}</code> resolves {@code card.html.twig}</li>
     *   <li><code>{% include 'card.html.twig' with {'title': item.title} %}</code> resolves {@code card.html.twig}</li>
     *   <li><code>{% embed 'layout.html.twig' with {'title': item.title} %}</code> resolves {@code layout.html.twig}</li>
     * </ul>
     */
    @NotNull
    private static Collection<String> getIncludeWithContextTemplateNames(@NotNull IncludeKeyContext context) {
        if (context.templateName() != null) {
            return Collections.singleton(context.templateName());
        }

        Collection<String> templates = new ArrayListSet<>();
        TwigUtil.visitTemplateIncludes(context.sourceElement(), include -> {
            TemplateInclude.TYPE type = include.getType();
            if (type == TemplateInclude.TYPE.INCLUDE || type == TemplateInclude.TYPE.EMBED) {
                templates.add(include.getTemplateName());
            }
        });

        if (templates.isEmpty() && context.sourceElement() instanceof TwigTagWithFileReference twigTagWithFileReference) {
            templates.addAll(TwigUtil.getIncludeTagStrings(twigTagWithFileReference));
        }

        return templates;
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
     * Ensures the caret token belongs to the key side of the current hash entry.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{'title': item}</code> matches {@code title}</li>
     *   <li><code>{'title': item}</code> does not match {@code item}</li>
     * </ul>
     */
    private static boolean isHashKey(@NotNull PsiElement hashLiteral, @NotNull PsiElement element) {
        PsiElement directChild = getDirectChild(hashLiteral, element);
        if (directChild == null || !isKeyToken(element, directChild)) {
            return false;
        }

        for (PsiElement previous = previousMeaningfulSibling(directChild); previous != null; previous = previousMeaningfulSibling(previous)) {
            if (isTwigElementType(previous, TwigTokenTypes.COLON)) {
                return false;
            }

            if (isTwigElementType(previous, TwigTokenTypes.COMMA) || isTwigElementType(previous, TwigTokenTypes.LBRACE_CURL)) {
                break;
            }
        }

        return true;
    }

    /**
     * Returns the direct child of {@code parent} that contains {@code element}.
     * <p>
     * Example: inside <code>{'title': item}</code>, a leaf inside {@code title} returns the
     * top-level hash child for the key.
     */
    @Nullable
    private static PsiElement getDirectChild(@NotNull PsiElement parent, @NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null && current.getParent() != parent) {
            current = current.getParent();
        }

        return current;
    }

    /**
     * Checks whether the current PSI token can represent an include/embed hash key.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{title: item}</code> matches {@code title}</li>
     *   <li><code>{"title": item}</code> matches {@code title} and its quote tokens</li>
     *   <li><code>{: item}</code> allows completion before the colon</li>
     * </ul>
     */
    private static boolean isKeyToken(@NotNull PsiElement element, @NotNull PsiElement directChild) {
        return isTwigElementType(element, TwigTokenTypes.STRING_TEXT) ||
            isTwigElementType(element, TwigTokenTypes.IDENTIFIER) ||
            isTwigElementType(directChild, TwigElementTypes.VARIABLE_REFERENCE) ||
            isTwigElementType(directChild, TwigTokenTypes.STRING_TEXT) ||
            isTwigElementType(directChild, TwigTokenTypes.IDENTIFIER) ||
            isTwigElementType(directChild, TwigTokenTypes.LBRACE_CURL) ||
            isTwigElementType(directChild, TwigTokenTypes.COLON) ||
            isTwigElementType(directChild, TwigTokenTypes.SINGLE_QUOTE) ||
            isTwigElementType(directChild, TwigTokenTypes.DOUBLE_QUOTE);
    }

    @Nullable
    private static IncludeKeyContext findExternalIncludeKeyContext(@NotNull PsiElement hashLiteral) {
        PsiElement sourceElement = PsiTreeUtil.findFirstParent(hashLiteral, false, parent ->
            isTwigElementType(parent, TwigElementTypes.TAG)
        );

        if (sourceElement == null || !isDirectWithHash(sourceElement, hashLiteral)) {
            return null;
        }

        String templateName = Arrays.stream(TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getExtensions()).map(extension -> {
            if (extension.isIncludeTemplate(sourceElement)) {
                Optional<String> includeTemplate = extension.getIncludeTemplate(sourceElement).stream().findFirst();
                if (includeTemplate.isPresent()) {
                    return includeTemplate.get();
                }
            }

            if (extension.isEmbedTemplate(sourceElement)) {
                return extension.getEmbedTemplate(sourceElement).stream().findFirst().orElse(null);
            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse(null);

        return templateName == null ? null : new IncludeKeyContext(sourceElement, templateName);
    }

    /**
     * Finds the include/embed tag that owns a direct {@code with} hash.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{% include 'card.html.twig' with {'title': item} %}</code> returns the include tag</li>
     *   <li><code>{% embed 'card.html.twig' with {'title': item} %}</code> returns the embed tag</li>
     * </ul>
     */
    @Nullable
    private static PsiElement findTagWithHash(@NotNull PsiElement hashLiteral) {
        PsiElement tag = PsiTreeUtil.findFirstParent(hashLiteral, false, parent ->
            isTwigElementType(parent, TwigElementTypes.INCLUDE_TAG) || isTwigElementType(parent, TwigElementTypes.EMBED_TAG)
        );

        return tag != null && isDirectWithHash(tag, hashLiteral) ? tag : null;
    }

    /**
     * Checks whether {@code hashLiteral} is the hash immediately after a tag {@code with}.
     * <p>
     * Example: in <code>{% include 'card.html.twig' with {'title': item} %}</code>, the
     * hash after {@code with} matches.
     */
    private static boolean isDirectWithHash(@NotNull PsiElement tag, @NotNull PsiElement hashLiteral) {
        for (PsiElement child = tag.getFirstChild(); child != null; child = nextMeaningfulSibling(child)) {
            if (!isWithKeyword(child)) {
                continue;
            }

            return nextMeaningfulSibling(child) == hashLiteral;
        }

        return false;
    }

    /**
     * Checks Twig's {@code with} keyword token, with a text fallback for parser variants.
     * <p>
     * Example: <code>{% include 'card.html.twig' with {'title': item} %}</code> matches {@code with}.
     */
    private static boolean isWithKeyword(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigTokenTypes.WITH_KEYWORD) || "with".equals(element.getText());
    }

    /**
     * Resolves the first template argument when {@code hashLiteral} belongs to an {@code include()} call.
     * <p>
     * Examples:
     * <ul>
     *   <li><code>{{ include('card.html.twig', {'title': item}) }}</code> returns {@code card.html.twig}</li>
     *   <li><code>{{ source('card.html.twig', {'title': item}) }}</code> returns {@code null}</li>
     * </ul>
     */
    @Nullable
    private static PsiElement findFunctionIncludeTemplateName(@NotNull PsiElement hashLiteral) {
        PsiElement functionCall = PsiElementUtils.getParentOfType(hashLiteral, TwigElementTypes.FUNCTION_CALL);
        PsiElement functionName = PsiElementUtils.getChildrenOfType(functionCall, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
        if (functionName == null || !"include".equals(functionName.getText())) {
            return null;
        }

        List<List<PsiElement>> arguments = splitFunctionArguments(functionCall);
        if (arguments.size() < 2) {
            return null;
        }

        PsiElement templateName = arguments.getFirst().stream()
            .filter(element -> isTwigElementType(element, TwigTokenTypes.STRING_TEXT))
            .findFirst()
            .orElse(null);
        if (templateName == null) {
            return null;
        }

        for (int i = 1; i < arguments.size(); i++) {
            if (findHashLiteral(arguments.get(i)) == hashLiteral) {
                return templateName;
            }
        }

        return null;
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
     * Gets the previous sibling while skipping PSI and Twig whitespace.
     */
    @Nullable
    private static PsiElement previousMeaningfulSibling(@NotNull PsiElement element) {
        PsiElement previous = element.getPrevSibling();
        while (previous != null && isWhitespace(previous)) {
            previous = previous.getPrevSibling();
        }

        return previous;
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
        ASTNode node = element.getNode();
        return node != null && node.getElementType() == elementType;
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

    /**
     * Matched include/embed hash-key context with an optionally resolved template name.
     */
    public record IncludeKeyContext(@NotNull PsiElement sourceElement, @Nullable String templateName) {
    }

    /**
     * Template variable exposed for include/embed {@code with} hash-key completion and navigation.
     *
     * @param name variable name, e.g. {@code title}
     * @param target PSI element where the variable was found in the target template
     * @param templateName target template that contributed the variable, e.g. {@code card.html.twig}
     */
    public record IncludeWithContextTemplateVariable(@NotNull String name, @NotNull PsiElement target, @NotNull String templateName) {
    }
}
