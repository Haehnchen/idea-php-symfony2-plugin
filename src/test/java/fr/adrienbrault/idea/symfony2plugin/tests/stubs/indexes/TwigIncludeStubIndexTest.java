package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.tests.templating.TestTwigFileUsage;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
 */
public class TwigIncludeStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getPoint().registerExtension(new TestTwigFileUsage(), getTestRootDisposable());

        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% include 'include_foo_quote.html.twig' %}\n" +
            "{% include \"include_foo_double_quote.html.twig\" %}\n" +
            "\n" +
            "{{ include('include_func_func_quote.html.twig') }}\n" +
            "{{ include(\"include_func_double_quote.html.twig\") }}\n" +
            "{{ include      (       'include_func_space.html.twig') }}\n" +
            "\n" +
            "{{ source('source_quote.html.twig') }}\n" +
            "{{ source(\"source_double_quote.html.twig\", ignore_missing = true) }}\n" +
            "\n" +
            "{% include ajax ? 'include_statement_0.html.twig' : 'include_statement_0.html.twig' %}\n" +
            "{% include ['include_array_0.html.twig', 'include_array_1.html.twig'] %}" +
            "\n" +
            "{% embed 'embed_foo_quote.html.twig' %}\n" +
            "{% embed \"embed_foo_double_quote.html.twig\" %}\n" +
            "\n" +
            "{% from 'from_foo_quote.html.twig' %}\n" +
            "\n" +
            "{% import 'import_foo_quote.html.twig' %}\n" +
            "\n" +
            "{% include '@!Foo/overwrite.html.twig' %}\n" +
            "\n" +
            "{% custom_include 'custom_include.html.twig' %}\n" +
            "{% custom_embed 'custom_embed.html.twig' %}\n" +
            "{% custom_import 'custom_import.html.twig' %}\n" +
            "{% custom_from 'custom_from.html.twig' %}\n" +
            "{% custom_source 'custom_source.html.twig' %}\n" +
            "\n" +
            "{% form_theme form.foobar with \"form_theme_1.html.twig\" %}" +
            "{% form_theme form.foobar with [\"form_theme_2.html.twig\", \"form_theme_3.html.twig\", \"form_theme_4.html.twig\"] %}"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex#getIndexer()
     */
    public void testTemplateIncludeIndexer() {
        assertIndexContains(TwigIncludeStubIndex.KEY,
            "include_foo_quote.html.twig", "include_foo_double_quote.html.twig", "include_func_func_quote.html.twig",
            "include_func_func_quote.html.twig", "source_quote.html.twig", "source_double_quote.html.twig",
            "include_func_space.html.twig", "include_statement_0.html.twig", "include_statement_0.html.twig",
            "include_array_0.html.twig", "include_array_1.html.twig", "embed_foo_quote.html.twig",
            "embed_foo_double_quote.html.twig", "from_foo_quote.html.twig", "import_foo_quote.html.twig",
            "@Foo/overwrite.html.twig", "custom_include.html.twig", "custom_embed.html.twig",
            "custom_import.html.twig", "custom_from.html.twig", "custom_source.html.twig",
            "form_theme_1.html.twig", "form_theme_2.html.twig", "form_theme_3.html.twig"
        );

        assertIndexContainsKeyWithValue(TwigIncludeStubIndex.KEY, "custom_include.html.twig", value -> value.getType() == TemplateInclude.TYPE.INCLUDE);
        assertIndexContainsKeyWithValue(TwigIncludeStubIndex.KEY, "custom_embed.html.twig", value -> value.getType() == TemplateInclude.TYPE.EMBED);
        assertIndexContainsKeyWithValue(TwigIncludeStubIndex.KEY, "custom_import.html.twig", value -> value.getType() == TemplateInclude.TYPE.IMPORT);
        assertIndexContainsKeyWithValue(TwigIncludeStubIndex.KEY, "custom_from.html.twig", value -> value.getType() == TemplateInclude.TYPE.FROM);
        assertIndexContainsKeyWithValue(TwigIncludeStubIndex.KEY, "custom_source.html.twig", value -> value.getType() == TemplateInclude.TYPE.INCLUDE_FUNCTION);
    }
}
