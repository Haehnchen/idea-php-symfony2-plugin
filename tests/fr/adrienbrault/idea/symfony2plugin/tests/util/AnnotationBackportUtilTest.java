package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;

public class AnnotationBackportUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see AnnotationBackportUtil#getAnnotationRouteName
     */
    public void testAnnotationRouteName() {
        assertEquals("my_page_so_good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my_page_so_good\""));
        assertEquals("my.page.so.good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my.page.so.good\""));
        assertEquals("my-page.so_good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my-page.so_good\""));
    }

    /**
     * @see AnnotationBackportUtil#getDefaultOrPropertyContents
     */
    public void testGetDefaultOrProperty() {
        PhpDocTag fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getDefaultOrPropertyContents(fromText, "foobar"));

        fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(foobar=\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getDefaultOrPropertyContents(fromText, "foobar"));

        fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(test={}, car=\"\"," +
            " foobar=\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getDefaultOrPropertyContents(fromText, "foobar"));
    }
}
