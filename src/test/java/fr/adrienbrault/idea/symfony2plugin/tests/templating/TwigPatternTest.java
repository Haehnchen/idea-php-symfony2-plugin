package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
 */
public class TwigPatternTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getBlockTagPattern
     */
    public void testGetBlockTagPattern() {
        String[] blocks = {
            "{% block 'a<caret>a' %}",
            "{% block \"a<caret>a\" %}",
            "{% block a<caret>a %}"
        };

        for (String s : blocks) {
            myFixture.configureByText(TwigFileType.INSTANCE, s);

            assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getBlockTagPattern().accepts(
                myFixture.getFile().findElementAt(myFixture.getCaretOffset()))
            );
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getBlockTagPattern
     */
    public void testGetAutocompletableAssetPattern() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ asset('bundles/<caret>test/img/' ~ entity.img ~ '.png') }}");
        assertFalse(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getAutocompletableAssetPattern().accepts(
            myFixture.getFile().findElementAt(myFixture.getCaretOffset()
            )));

        myFixture.configureByText(TwigFileType.INSTANCE, "{{ asset('bundles/<caret>test/img/') }}");
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getAutocompletableAssetPattern().accepts(
            myFixture.getFile().findElementAt(myFixture.getCaretOffset())
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getPrintBlockOrTagFunctionPattern
     */
    public void testGetPrintBlockOrTagFunctionPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPrintBlockOrTagFunctionPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar('f<caret>o') }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPrintBlockOrTagFunctionPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{% if foobar('f<caret>o') %}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPrintBlockOrTagFunctionPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{% set foo = foobar('f<caret>o') %}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPrintBlockOrTagFunctionPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{% elseif foobar('f<caret>o') %}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPrintBlockOrTagFunctionPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{% else foobar('f<caret>o') %}")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getFunctionPattern
     */
    public void testgetFunctionPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionPattern("form").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ for<caret>m(test) }}").getParent()
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionPattern("form").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ for<caret>m     (test) }}").getParent()
        ));

        assertFalse(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionPattern("f").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ for<caret>m(test) }}").getParent()
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getFunctionWithFirstParameterAsArrayPattern
     */
    public void testGetFunctionWithFirstParameterAsArrayPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsArrayPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(['fo<caret>o']) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsArrayPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(['foo', 'fo<caret>o']) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsArrayPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar([\"foo\", \"fo<caret>o\"]) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsArrayPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar       (       [ 'foo'     , 'fo<caret>o']) }}")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getFunctionWithFirstParameterAsLiteralPattern
     */
    public void testFunctionWithFirstParameterAsLiteralPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'f<caret>o'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar    (       {     'f<caret>o'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'foo', 'f<caret>o'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({\"f<caret>o\"}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({\"foo\", \"f<caret>o\"}) }}")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getFunctionWithFirstParameterAsKeyLiteralPattern
     */
    public void testGetFunctionWithFirstParameterAsKeyLiteralPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'foo': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'foo': 'foobar', 'fo': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'foo': 'foobar'  ~ 'foobar' , 'f<caret>o': 'foobar'}) }}")
        ));

        assertFalse(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({'fo': 'f<caret>d'}) }}")
        ));

        assertFalse(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar({}, 'a<caret>a'}")
        ));

        assertFalse(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar( {foo({}, 'a<caret>a'}} )")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getFunctionWithSecondParameterAsKeyLiteralPattern
     */
    public void testGetFunctionWithSecondParameterAsKeyLiteralPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(12, {'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(12, {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(foo.foo, {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar('foo', {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar(\"foo\", {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("foobar").accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ foobar('f' ~ 'oo', {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getPathAfterLeafPattern
     */
    public void testGetPathAfterLeafPattern() {
        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPathAfterLeafPattern().accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f<caret>o'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPathAfterLeafPattern().accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'foobar': 'foobar', 'f<caret>o': 'foobar'}) }}")
        ));

        assertTrue(fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getPathAfterLeafPattern().accepts(
            findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f': 'f', 'f': 'f', 'f<caret>a': 'f'}) }}")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getParameterAsStringPattern
     */
    public void testGetParameterAsStringPattern() {
        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getParameterAsStringPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getParameterAsStringPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', 'f<caret>o', null) }}"))
        );

        assertFalse(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getParameterAsStringPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ path('f<caret>o') }}"))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getTransDomainPattern
     */
    public void testGetTransDomainPattern() {
        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|trans({}, 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|trans([], 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|trans(null, 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|transchoice(2, {}, 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|transchoice(2, null, 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|transchoice(2, [], 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|transchoice(test.test, [], 'f<caret>o') }}"))
        );

        assertTrue(
            fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.getTransDomainPattern().accepts(findElementAt(TwigFileType.INSTANCE, "{{ ''|transchoice(test ~ 'test', [], 'f<caret>o') }}"))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern#getTranslationKeyPattern
     */
    public void testGetTranslationPattern() {
        assertTrue(TwigPattern.getTranslationKeyPattern("trans").accepts(findElementAt(TwigFileType.INSTANCE, "{{ 'f<caret>oo'|trans }}")));
        assertTrue(TwigPattern.getTranslationKeyPattern("trans").accepts(findElementAt(TwigFileType.INSTANCE, "{{ 'f<caret>oo'|trans('foobar') }}")));
    }
}
