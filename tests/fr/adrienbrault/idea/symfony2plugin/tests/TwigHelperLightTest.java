package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementFactory;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getIncludeTagStrings
 * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getBlocksInFile
 */
public class TwigHelperLightTest extends SymfonyLightCodeInsightFixtureTestCase {

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

    public void testBlocksInFileCollector() {
        assertEquals("foo", buildBlocks("{% block foo %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block \"foo\" %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block 'foo' %}").iterator().next().getName());

        assertEquals("foo", buildBlocks("{%- block foo -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block \"foo\" -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block 'foo' -%}").iterator().next().getName());

        assertNotNull(buildBlocks("{%- block 'foo' -%}").iterator().next().getPsiFile());
        assertSize(1, buildBlocks("{%- block 'foo' -%}").iterator().next().getBlock());
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
     * @see TwigHelper#getBlockTagPattern
     */
    public void testGetBlockTagPattern() {
        String[] blocks = {
            "{% block 'a<caret>a' %}",
            "{% block \"a<caret>a\" %}",
            "{% block a<caret>a %}"
        };

        for (String s : blocks) {
            myFixture.configureByText(TwigFileType.INSTANCE, s);

            assertTrue(TwigHelper.getBlockTagPattern().accepts(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()))
            );
        }
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
