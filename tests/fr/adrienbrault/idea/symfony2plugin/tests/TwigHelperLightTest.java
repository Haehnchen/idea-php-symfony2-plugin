package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.psi.PsiFileFactory;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementFactory;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.TwigHelper#getTwigExtendsTagTemplates
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

        assertNotNull("foo", buildBlocks("{%- block 'foo' -%}").iterator().next().getPsiFile());
        assertSize(1, buildBlocks("{%- block 'foo' -%}").iterator().next().getBlock());
    }

    private Collection<String> buildExtendsTagList(String string) {
        return TwigHelper.getTwigExtendsTagTemplates((TwigExtendsTag) TwigElementFactory.createPsiElement(getProject(), string, TwigElementTypes.EXTENDS_TAG));
    }

    private Collection<TwigBlock> buildBlocks(String content) {
        return TwigHelper.getBlocksInFile((TwigFile) PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + TwigFileType.INSTANCE.getDefaultExtension(), TwigFileType.INSTANCE, content, System.currentTimeMillis(), false));
    }
}
