package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacroTagInterface;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see TwigHelper#getIncludeTagStrings
 * @see TwigHelper#getBlocksInFile
 */
public class TwigHelperTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testSingleExtends() {
        assertEquals("::base.html.twig", buildExtendsTagList("{% extends '::base.html.twig' %}").iterator().next());
        assertEquals("::base.html.twig", buildExtendsTagList("{% extends \"::base.html.twig\" %}").iterator().next());
        assertEquals("::base.html.twig", buildExtendsTagList("{%extends \"::base.html.twig\"%}").iterator().next());
    }

    public void testSingleExtendsInvalid() {
        assertSize(0, buildExtendsTagList("{% extends ~ '::base.html.twig' %}"));
        assertSize(0, buildExtendsTagList("{% extends foo ~ '::base.html.twig' %}"));
    }

    public void testSingleBlank() {
        assertSize(0, buildExtendsTagList("{% extends '' %}"));
        assertSize(0, buildExtendsTagList("{% extends \"\" %}"));
   }

    public void testConditional() {
        Collection<String> twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" : \"base.html\" %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? 'base_ajax.html' : 'base.html' %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{%extends request.ajax?'base_ajax.html':'base.html'%}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));
    }

    public void testConditionalBlank() {
        assertSize(0, buildExtendsTagList("{% extends ? '' : '' %}"));
        assertSize(0, buildExtendsTagList("{% extends ? \"\" : \"\" %}"));
    }

    public void testConditionalInvalidGiveBestPossibleStrings() {
        Collection<String> twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? foo ~ \"base_ajax.html\" : \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" ~ test : \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? foo ~ \"base_ajax.html\" : ~ \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertFalse(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" : ~ \"base.html\" %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertFalse(twigExtendsValues.contains("base.html"));
    }

    /**
     * @see TwigHelper#getBlocksInFile
     */
    public void testBlocksInFileCollector() {
        assertEquals("foo", buildBlocks("{% block foo %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block \"foo\" %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block 'foo' %}").iterator().next().getName());

        assertEquals("foo", buildBlocks("{%- block foo -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block \"foo\" -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block 'foo' -%}").iterator().next().getName());

        assertNotNull(buildBlocks("{%- block 'foo' -%}").iterator().next().getPsiFile());
        assertSize(1, buildBlocks("{%- block 'foo' -%}").iterator().next().getBlock());

        assertEquals("foobar_block", buildBlocks("{{ block('foobar_block') }}").iterator().next().getName());
        assertEquals("foobar_block", buildBlocks("{{ block(\"foobar_block\") }}").iterator().next().getName());
    }

    /**
     * @see TwigHelper#getIncludeTagStrings
     */
    public void testIncludeTagStrings() {

        assertEqual(getIncludeTemplates("{% include 'foo.html.twig' %}"), "foo.html.twig");
        assertSize(0, getIncludeTemplates("{% include '' %}"));
        assertEqual(getIncludeTemplates("{% include ['foo.html.twig'] %}"), "foo.html.twig");
        assertEqual(getIncludeTemplates("{% include ['foo.html.twig',''] %}"), "foo.html.twig");

        assertEqual(getIncludeTemplates(
                "{% include ['foo.html.twig', 'foo_1.html.twig', , ~ 'foo_2.html.twig', 'foo_3.html.twig' ~, 'foo' ~ 'foo_4.html.twig', 'end.html.twig'] %}"),
            "foo.html.twig", "foo_1.html.twig", "end.html.twig"
        );
    }

    /**
     * @see TwigHelper#getIncludeTagStrings
     */
    public void testIncludeTagStringsTernary() {
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' : \"include_statement_1.html.twig\" %}"), "include_statement_0.html.twig", "include_statement_1.html.twig");
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statem' ~ 'aa' ~ 'ent_0.html.twig' : 'include_statement_1.html.twig' %}"), "include_statement_1.html.twig");
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' : ~ 'include_statement_1.html.twig' %}"), "include_statement_0.html.twig");
        assertSize(0, getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' ~ : ~ 'include_statement_1.html.twig' %}"));
    }

    /**
     * @see TwigHelper#getIncludeTagStrings
     */
    public void testIncludeTagNonAllowedTags() {
        assertSize(0, getIncludeTemplates("{% from 'foo.html.twig' %}", TwigElementTypes.IMPORT_TAG));
        assertSize(0, getIncludeTemplates("{% import 'foo.html.twig' %}", TwigElementTypes.IMPORT_TAG));
    }

    /**
     * @see TwigHelper#getTwigPathFromYamlConfig
     */
    public void testGetTwigPathFromYamlConfig() {
        String content = "twig:\n" +
            "    paths:\n" +
            "        \"%kernel.root_dir%/../src/views\": core\n" +
            "        \"%kernel.root_dir%/../src/views2\": 'core2'\n" +
            "        \"%kernel.root_dir%/../src/views3\": \"core3\"\n" +
            "        \"%kernel.root_dir%/../src/views4\": ~\n" +
            "        \"%kernel.root_dir%/../src/views5\": \n" +
            "        \"%kernel.root_dir%/..//src/views\": core8\n" +
            "        \"%kernel.root_dir%/..\\src/views\": core9\n" +
            "        \"%kernel.root_dir%/..\\src/views10\": '!core10'\n" +
            "        \"%kernel.root_dir%/..\\src/views11\": !core11\n"
            ;

        YAMLFile fileFromText = (YAMLFile) PsiFileFactory.getInstance(getProject())
            .createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content, System.currentTimeMillis(), false);

        Collection<Pair<String, String>> map = TwigHelper.getTwigPathFromYamlConfig(fileFromText);

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core2") && pair.getSecond().equals("%kernel.root_dir%/../src/views2")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core3") && pair.getSecond().equals("%kernel.root_dir%/../src/views3")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("") && pair.getSecond().equals("%kernel.root_dir%/../src/views4")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("") && pair.getSecond().equals("%kernel.root_dir%/../src/views5")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core8") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core9") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core10") && pair.getSecond().equals("%kernel.root_dir%/../src/views10")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core11") && pair.getSecond().equals("%kernel.root_dir%/../src/views11")
        ));
    }
    /**
     * @see TwigHelper#getTwigGlobalsFromYamlConfig
     */
    public void testGetTwigGlobalsFromYamlConfig() {
        String content = "twig:\n" +
            "    globals:\n" +
            "       ga_tracking: '%ga_tracking%'\n" +
            "       user_management: '@AppBundle\\Service\\UserManagement'\n"
            ;

        YAMLFile yamlFile = (YAMLFile) PsiFileFactory.getInstance(getProject())
            .createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content, System.currentTimeMillis(), false);

        Map<String, String> globals = TwigHelper.getTwigGlobalsFromYamlConfig(yamlFile);

        assertEquals("%ga_tracking%", globals.get("ga_tracking"));
        assertEquals("@AppBundle\\Service\\UserManagement", globals.get("user_management"));
    }

    /**
     * @see TwigHelper#getUniqueTwigTemplatesList
     */
    public void testGetUniqueTwigTemplatesList() {
        assertSize(1, TwigHelper.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path/", "path"),
            new TwigPath("path\\", "path")
        )));

        assertSize(1, TwigHelper.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path", "path"),
            new TwigPath("path", "path")
        )));

        assertSize(2, TwigHelper.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path/a", "path"),
            new TwigPath("foobar", "path")
        )));

        assertSize(2, TwigHelper.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path", "path", TwigPathIndex.NamespaceType.BUNDLE),
            new TwigPath("path", "path")
        )));
    }

    /**
     * @see TwigHelper#getMatchingRouteNameOnParameter
     */
    public void testGetMatchingRouteNameOnParameter() {
        assertEquals(
            "foo",
            TwigHelper.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f<caret>o'}) }}"))
        );

        assertEquals(
            "foo",
            TwigHelper.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f': 'f', 'f': 'f', 'f<caret>a': 'f'}) }}"))
        );

        assertNull(
            TwigHelper.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo' ~ 'foo', {'f<caret>o'}) }}"))
        );
    }

    /**
     * @see TwigHelper#normalizeTemplateName
     */
    public void testNormalizeTemplateName() {
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle:Foo:steps/step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle:Foo/steps:step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle::Foo/steps/step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("@BarBundle/Foo/steps:step_finish.html.twig"));
        assertEquals("step_finish.html.twig", TwigHelper.normalizeTemplateName("step_finish.html.twig"));

        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle:Foo:steps\\step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("@BarBundle\\Foo/steps:step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("@!BarBundle\\Foo/steps:step_finish.html.twig"));
    }

    /**
     * @see TwigHelper#getDomainFromTranslationTag
     */
    public void testGetDomainFromTranslationTag() {
        assertEquals("app", TwigHelper.getDomainFromTranslationTag(
            (TwigCompositeElement) TwigElementFactory.createPsiElement(getProject(), "{% trans with {'%name%': 'Fabien'} from \"app\" %}", TwigElementTypes.TAG)
        ));

        assertEquals("app", TwigHelper.getDomainFromTranslationTag(
            (TwigCompositeElement) TwigElementFactory.createPsiElement(getProject(), "{% transchoice count with {'%name%': 'Fabien'} from 'app' %}", TwigElementTypes.TAG)
        ));
    }

    public void testGetMacros() {
        PsiFile psiFile = myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% macro input(name, value, type, size) %}{% endmacro %}\n" +
            "{% macro foobar %}{% endmacro %}\n" +
            "{% if foobar %}{% macro foobar_if %}{% endmacro %}{% endif %}"
        );

        Collection<TwigMacroTagInterface> macros = TwigUtil.getMacros(psiFile);

        assertEquals(
            "(name, value, type, size)",
            macros.stream().filter(twigMacroTag -> "input".equals(twigMacroTag.getName())).findFirst().get().getParameters()
        );

        assertNull(
            macros.stream().filter(twigMacroTag -> "foobar".equals(twigMacroTag.getName())).findFirst().get().getParameters()
        );

        assertNull(
            macros.stream().filter(twigMacroTag -> "foobar_if".equals(twigMacroTag.getName())).findFirst().get().getParameters()
        );
    }

    private void assertEqual(Collection<String> c, String... values) {
        if(!StringUtils.join(c, ",").equals(StringUtils.join(Arrays.asList(values), ","))) {
            fail(String.format("Fail that '%s' is equal '%s'", StringUtils.join(c, ","), StringUtils.join(Arrays.asList(values), ",")));
        }
    }

    private Collection<String> buildExtendsTagList(String string) {
        return TwigHelper.getTwigExtendsTagTemplates((TwigExtendsTag) TwigElementFactory.createPsiElement(getProject(), string, TwigElementTypes.EXTENDS_TAG));
    }

    private Collection<TwigBlock> buildBlocks(String content) {
        return TwigHelper.getBlocksInFile((TwigFile) PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + TwigFileType.INSTANCE.getDefaultExtension(), TwigFileType.INSTANCE, content, System.currentTimeMillis(), false));
    }

    private Collection<String> getIncludeTemplates(@NotNull String content) {
        return getIncludeTemplates(content, TwigElementTypes.INCLUDE_TAG);
    }

    private Collection<String> getIncludeTemplates(@NotNull String content, @NotNull final IElementType type) {
        return TwigHelper.getIncludeTagStrings((TwigTagWithFileReference) TwigElementFactory.createPsiElement(getProject(), content, type));
    }
}
