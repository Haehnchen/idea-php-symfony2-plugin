package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionArgument;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil.ShapeKey;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Completes hash keys and literal string values from the PHPDoc type of the PHP parameter behind a Twig function,
 * and the variant names of a {@code cva()} call.
 * <p>
 * Examples:
 * <ul>
 *   <li><code>{{ button({ <caret> }) }}</code></li>
 *   <li><code>{{ button({ size: '<caret>' }) }}</code></li>
 *   <li><code>{{ button(options: { <caret> }) }}</code></li>
 *   <li><code>{{ cva(variants: { size: { sm: {} } }, defaultVariants: { <caret> }) }}</code></li>
 * </ul>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class TwigFunctionHashCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final Pattern TWIG_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();
        if (!Symfony2ProjectComponent.isEnabled(position)) {
            return;
        }

        FunctionHash hash = TwigFunctionHashParser.parse(position);
        if (hash == null) {
            return;
        }

        Project project = position.getProject();
        TwigExtension twigExtension = TwigExtensionParser.getFunctions(project).get(hash.functionName());
        if (twigExtension == null) {
            return;
        }

        int parameterShift = getParameterShift(twigExtension);

        Map<String, LookupElement> lookupElements = new LinkedHashMap<>();
        for (PhpNamedElement element : PhpElementsUtil.getPsiElementsBySignature(project, twigExtension.getSignature())) {
            if (!(element instanceof Function function)) {
                continue;
            }

            Integer parameterIndex = getParameterIndex(function, hash, parameterShift);
            if (parameterIndex == null) {
                continue;
            }

            List<LookupElement> functionLookupElements = new ArrayList<>(createLookupElements(function, parameterIndex, hash));
            functionLookupElements.addAll(createCvaLookupElements(function, parameterIndex, parameterShift, hash));

            for (LookupElement lookupElement : functionLookupElements) {
                lookupElements.putIfAbsent(lookupElement.getLookupString(), lookupElement);
            }
        }

        CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(parameters, resultSet);
        completionResultSet.addAllElements(lookupElements.values());
    }

    private static int getParameterShift(@NotNull TwigExtension twigExtension) {
        int shift = 0;
        if ("true".equals(twigExtension.getOption("needs_environment"))) {
            shift++;
        }

        if ("true".equals(twigExtension.getOption("needs_context"))) {
            shift++;
        }

        return shift;
    }

    /**
     * Resolves a named argument by name, a positional one after the environment and context parameters.
     */
    @Nullable
    private static Integer getParameterIndex(@NotNull Function function, @NotNull FunctionHash hash, int parameterShift) {
        String argumentName = hash.argumentName();
        if (argumentName == null) {
            return hash.argumentIndex() + parameterShift;
        }

        int index = getParameterIndex(function, argumentName);
        return index >= 0 ? index : null;
    }

    private static int getParameterIndex(@NotNull Function function, @NotNull String name) {
        Parameter[] parameters = function.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (name.equals(parameters[i].getName())) {
                return i;
            }
        }

        return -1;
    }

    @NotNull
    private static List<LookupElement> createLookupElements(@NotNull Function function, int parameterIndex, @NotNull FunctionHash hash) {
        List<LookupElement> lookupElements = new ArrayList<>();

        String valueKey = hash.valueKey();
        if (valueKey != null) {
            for (String value : PhpDocArrayShapeUtil.getParameterLiteralValues(function, parameterIndex, hash.keyPath(), valueKey)) {
                lookupElements.add(createValueLookupElement(value));
            }

            return lookupElements;
        }

        for (ShapeKey key : PhpDocArrayShapeUtil.getParameterKeys(function, parameterIndex, hash.keyPath())) {
            lookupElements.add(createKeyLookupElement(key, hash.quoted()));
        }

        return lookupElements;
    }

    /**
     * Completes the variant names declared in the {@code variants} argument of the same call, in the default variants
     * and the compound variant items of <code>cva()</code> (symfony/ux-css) and <code>html_cva()</code> (twig/html-extra).
     * <p>
     * Examples:
     * <ul>
     *   <li><code>cva(variants: { size: { sm: {} } }, defaultVariants: { <caret> })</code> completes {@code size}</li>
     *   <li><code>cva(variants: { size: { sm: {} } }, compoundVariants: [{ size: '<caret>' }])</code> completes {@code sm}</li>
     * </ul>
     */
    @NotNull
    private static List<LookupElement> createCvaLookupElements(@NotNull Function function, int parameterIndex, int parameterShift, @NotNull FunctionHash hash) {
        Parameter[] parameters = function.getParameters();
        int variantsIndex = getParameterIndex(function, "variants");
        boolean isCva = variantsIndex >= 0 &&
            getParameterIndex(function, "compoundVariants") >= 0 &&
            (getParameterIndex(function, "defaultVariants") >= 0 || getParameterIndex(function, "defaultVariant") >= 0);

        if (!isCva || parameterIndex >= parameters.length) {
            return Collections.emptyList();
        }

        List<String> keyPath = hash.keyPath();
        String valueKey = hash.valueKey();
        String parameterName = parameters[parameterIndex].getName();

        boolean isVariantKey;
        String variantName;
        if ("defaultVariants".equals(parameterName) || "defaultVariant".equals(parameterName)) {
            isVariantKey = keyPath.isEmpty() && valueKey == null;
            variantName = keyPath.isEmpty() && !PhpDocArrayShapeUtil.LIST_ITEM.equals(valueKey) ? valueKey : null;
        } else if ("compoundVariants".equals(parameterName)) {
            boolean isItem = keyPath.equals(List.of(PhpDocArrayShapeUtil.LIST_ITEM));
            isVariantKey = isItem && valueKey == null;
            if (isItem && !PhpDocArrayShapeUtil.LIST_ITEM.equals(valueKey)) {
                variantName = valueKey;
            } else if (keyPath.size() == 2 && PhpDocArrayShapeUtil.LIST_ITEM.equals(keyPath.get(0)) && PhpDocArrayShapeUtil.LIST_ITEM.equals(valueKey)) {
                variantName = keyPath.get(1);
            } else {
                variantName = null;
            }
        } else {
            return Collections.emptyList();
        }

        if (!isVariantKey && variantName == null) {
            return Collections.emptyList();
        }

        PsiElement variants = findArgumentValue(hash.functionCall(), "variants", variantsIndex - parameterShift);
        Map<String, PsiElement> variantEntries = variants != null ? TwigFunctionHashParser.getHashEntries(variants) : Collections.emptyMap();

        List<LookupElement> lookupElements = new ArrayList<>();
        if (isVariantKey) {
            for (String name : variantEntries.keySet()) {
                lookupElements.add(createKeyLookupElement(new ShapeKey(name, true), hash.quoted()));
            }

            return lookupElements;
        }

        PsiElement variantValues = variantEntries.get(variantName);
        if (variantValues != null) {
            for (String value : TwigFunctionHashParser.getHashEntries(variantValues).keySet()) {
                lookupElements.add(createValueLookupElement(value));
            }
        }

        return lookupElements;
    }

    /**
     * Finds an argument by name, or at its position when it is passed positionally.
     */
    @Nullable
    private static PsiElement findArgumentValue(@NotNull PsiElement functionCall, @NotNull String name, int index) {
        Collection<FunctionArgument> arguments = TwigFunctionHashParser.getArguments(functionCall);

        FunctionArgument argument = ContainerUtil.find(arguments, candidate -> name.equals(candidate.name()));
        if (argument == null) {
            argument = ContainerUtil.find(arguments, candidate -> candidate.name() == null && candidate.index() == index);
        }

        return argument != null ? argument.value() : null;
    }

    @NotNull
    private static LookupElement createValueLookupElement(@NotNull String value) {
        return LookupElementBuilder.create(value).withIcon(Symfony2Icons.SYMFONY);
    }

    @NotNull
    private static LookupElement createKeyLookupElement(@NotNull ShapeKey key, boolean quoted) {
        String name = key.name();
        boolean needsQuotes = !quoted && !TWIG_NAME.matcher(name).matches();
        String lookupString = needsQuotes ? "'" + name + "'" : name;

        return LookupElementBuilder.create(lookupString)
            .withLookupString(name)
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(!key.optional());
    }
}
