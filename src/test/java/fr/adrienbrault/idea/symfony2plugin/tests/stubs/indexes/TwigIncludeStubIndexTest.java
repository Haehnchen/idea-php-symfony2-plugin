package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
 */
public class TwigIncludeStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

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
            "@Foo/overwrite.html.twig",
            "form_theme_1.html.twig", "form_theme_2.html.twig", "form_theme_3.html.twig"
        );
    }
    public void testAllTypesForOneTemplateInOneFile() {
        PsiFile caller = myFixture.addFileToProject("all.html.twig", """
            {% include 'shared.html.twig' %}
            {{ include('shared.html.twig') }}
            {% embed 'shared.html.twig' %}{% endembed %}
            {% include 'shared.html.twig' %}
            {{ source('shared.html.twig') }}
            {% form_theme form 'shared.html.twig' %}
            {% import 'shared.html.twig' as macros %}
            {% from 'shared.html.twig' import field %}
            {{ block('content', 'shared.html.twig') }}
            """);

        assertEquals(EnumSet.allOf(TYPE.class), usages("shared.html.twig").get(caller.getVirtualFile()).getTypes());
    }

    public void testNormalizationRepetitionAndOrder() {
        PsiFile first = myFixture.addFileToProject("first.html.twig", """
            {% include '@!Foo/shared.html.twig' %}
            {% embed '@Foo/shared.html.twig' %}{% endembed %}
            {% include '@Foo/shared.html.twig' %}
            """);
        PsiFile second = myFixture.addFileToProject("second.html.twig", """
            {% embed '@!Foo/shared.html.twig' %}{% endembed %}
            {% include '@Foo/shared.html.twig' %}
            """);
        PsiFile third = myFixture.addFileToProject("third.html.twig", "{{ source('@Foo/shared.html.twig') }}");

        Map<VirtualFile, TemplateInclude> usages = usages("@Foo/shared.html.twig");
        assertEquals(3, usages.size());

        TemplateInclude value = usages.get(first.getVirtualFile());

        assertEquals("@Foo/shared.html.twig", value.getTemplate());
        assertEquals(Set.of(TYPE.INCLUDE, TYPE.EMBED), value.getTypes());
        assertEquals(value, usages.get(second.getVirtualFile()));
        assertEquals(Set.of(TYPE.SOURCE_FUNCTION), usages.get(third.getVirtualFile()).getTypes());
    }

    public void testFileChangesRemoveTypesAndUsages() {
        PsiFile caller = myFixture.configureByText("changing.html.twig", """
            {% include 'shared.html.twig' %}
            {% embed 'shared.html.twig' %}{% endembed %}
            """);

        assertEquals(Set.of(TYPE.INCLUDE, TYPE.EMBED), usages("shared.html.twig").get(caller.getVirtualFile()).getTypes());

        myFixture.configureByText("changing.html.twig", "{% include 'shared.html.twig' %}");
        assertEquals(Set.of(TYPE.INCLUDE), usages("shared.html.twig").get(caller.getVirtualFile()).getTypes());

        myFixture.configureByText("changing.html.twig", "");
        assertTrue(usages("shared.html.twig").isEmpty());

        myFixture.configureByText("changing.html.twig", "{{ source('shared.html.twig') }}");
        assertEquals(Set.of(TYPE.SOURCE_FUNCTION), usages("shared.html.twig").get(caller.getVirtualFile()).getTypes());
        WriteCommandAction.runWriteCommandAction(getProject(), caller::delete);
        assertTrue(usages("shared.html.twig").isEmpty());
    }

    private Map<VirtualFile, TemplateInclude> usages(String name) {
        Map<VirtualFile, TemplateInclude> usages = new HashMap<>();
        FileBasedIndex.getInstance().processValues(TwigIncludeStubIndex.KEY, name, null, (file, value) -> {
            usages.put(file, value);
            return true;
        }, GlobalSearchScope.projectScope(getProject()));
        return usages;
    }

}
