package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil.ShapeKey;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Completes hash keys and literal string values from the PHPDoc array shape of the PHP parameter behind a Twig function.
 * <p>
 * Examples:
 * <ul>
 *   <li><code>{{ button({ <caret> }) }}</code></li>
 *   <li><code>{{ button({ size: '<caret>' }) }}</code></li>
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

        int parameterIndex = getParameterIndex(twigExtension, hash.argumentIndex());

        Map<String, LookupElement> lookupElements = new LinkedHashMap<>();
        for (PhpNamedElement element : PhpElementsUtil.getPsiElementsBySignature(project, twigExtension.getSignature())) {
            if (!(element instanceof Function function)) {
                continue;
            }

            for (LookupElement lookupElement : createLookupElements(function, parameterIndex, hash)) {
                lookupElements.putIfAbsent(lookupElement.getLookupString(), lookupElement);
            }
        }

        CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(parameters, resultSet);
        completionResultSet.addAllElements(lookupElements.values());
    }

    private static int getParameterIndex(@NotNull TwigExtension twigExtension, int argumentIndex) {
        int parameterIndex = argumentIndex;
        if ("true".equals(twigExtension.getOption("needs_environment"))) {
            parameterIndex++;
        }

        if ("true".equals(twigExtension.getOption("needs_context"))) {
            parameterIndex++;
        }

        return parameterIndex;
    }

    @NotNull
    private static List<LookupElement> createLookupElements(@NotNull Function function, int parameterIndex, @NotNull FunctionHash hash) {
        List<LookupElement> lookupElements = new ArrayList<>();

        String valueKey = hash.valueKey();
        if (valueKey != null) {
            for (String value : PhpDocArrayShapeUtil.getParameterLiteralValues(function, parameterIndex, hash.keyPath(), valueKey)) {
                lookupElements.add(LookupElementBuilder.create(value).withIcon(Symfony2Icons.SYMFONY));
            }

            return lookupElements;
        }

        for (ShapeKey key : PhpDocArrayShapeUtil.getParameterKeys(function, parameterIndex, hash.keyPath())) {
            lookupElements.add(createKeyLookupElement(key, hash.quoted()));
        }

        return lookupElements;
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
