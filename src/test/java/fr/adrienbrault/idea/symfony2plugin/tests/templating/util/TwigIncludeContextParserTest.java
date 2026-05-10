package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigIncludeContextParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class TwigIncludeContextParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testTagIncludeWithOnlyParsesArguments() {
        TwigIncludeContextParser.IncludeContext context = parseTagInclude("{% include 'partials/_card.html.twig' with {'title': 'Example', product: item.product} only %}");

        assertFalse(context.withParentContext());
        assertArgumentNames(context, "title", "product");
        assertEquals("'Example'", getArgumentValueText(context, "title"));
        assertEquals("item.product", getArgumentValueText(context, "product"));
    }

    public void testTagIncludeWithIgnoreMissingKeepsParentContext() {
        TwigIncludeContextParser.IncludeContext context = parseTagInclude("{% include 'sidebar.html.twig' ignore missing with {'name': 'Fabien'} %}");

        assertTrue(context.withParentContext());
        assertArgumentNames(context, "name");
        assertEquals("'Fabien'", getArgumentValueText(context, "name"));
    }

    public void testTagIncludeSupportsQuotedKeys() {
        TwigIncludeContextParser.IncludeContext context = parseTagInclude("{% include 'partials/_card.html.twig' with {'itemAlias': item, \"itemLabel\": item.label} %}");

        assertTrue(context.withParentContext());
        assertArgumentNames(context, "itemAlias", "itemLabel");
        assertEquals("item", getArgumentValueText(context, "itemAlias"));
        assertEquals("item.label", getArgumentValueText(context, "itemLabel"));
    }

    public void testFunctionIncludeWithContextFalseParsesArguments() {
        TwigIncludeContextParser.IncludeContext context = parseFunctionInclude("{{ include('partials/_card.html.twig', {title: 'Example', product: item.product}, with_context: false) }}");

        assertFalse(context.withParentContext());
        assertArgumentNames(context, "title", "product");
        assertEquals("'Example'", getArgumentValueText(context, "title"));
        assertEquals("item.product", getArgumentValueText(context, "product"));
    }

    public void testFunctionIncludeWithContextFalseEqualsWithoutArguments() {
        TwigIncludeContextParser.IncludeContext context = parseFunctionInclude("{{ include('partials/_card.html.twig', with_context = false) }}");

        assertFalse(context.withParentContext());
        assertTrue(context.arguments().isEmpty());
    }

    public void testFunctionIncludeSupportsQuotedKeysAndComplexValues() {
        TwigIncludeContextParser.IncludeContext context = parseFunctionInclude("{{ include('partials/_card.html.twig', {'itemAlias': item, \"itemLabel\": item.label, options: {label: item.name, flags: {'active': true}}}) }}");

        assertTrue(context.withParentContext());
        assertArgumentNames(context, "itemAlias", "itemLabel", "options");
        assertEquals("item", getArgumentValueText(context, "itemAlias"));
        assertEquals("item.label", getArgumentValueText(context, "itemLabel"));
        assertTrue(getArgumentValueText(context, "options").contains("item.name"));
    }

    private TwigIncludeContextParser.IncludeContext parseTagInclude(String content) {
        myFixture.configureByText(TwigFileType.INSTANCE, content);

        TwigTagWithFileReference tag = PsiTreeUtil.findChildOfType(myFixture.getFile(), TwigTagWithFileReference.class);
        assertNotNull(tag);

        return TwigIncludeContextParser.resolveTagIncludeContext(tag);
    }

    private TwigIncludeContextParser.IncludeContext parseFunctionInclude(String content) {
        myFixture.configureByText(TwigFileType.INSTANCE, content);

        PsiElement[] templateNames = PsiTreeUtil.collectElements(myFixture.getFile(), TwigPattern.getPrintBlockOrTagFunctionPattern("include")::accepts);
        assertEquals(1, templateNames.length);

        return TwigIncludeContextParser.resolveFunctionIncludeContext(templateNames[0]);
    }

    private void assertArgumentNames(TwigIncludeContextParser.IncludeContext context, String... expected) {
        assertEquals(new HashSet<>(Arrays.asList(expected)), context.argumentNames());
    }

    private String getArgumentValueText(TwigIncludeContextParser.IncludeContext context, String name) {
        return context.arguments().stream()
            .filter(argument -> name.equals(argument.name()))
            .findFirst()
            .map(argument -> argument.valueElements().stream().map(PsiElement::getText).collect(Collectors.joining("")))
            .orElseGet(() -> {
                fail("Missing include argument: " + name);
                return "";
            });
    }
}
