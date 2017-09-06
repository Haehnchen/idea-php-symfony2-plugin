package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;

public class AnnotationBackportUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see AnnotationBackportUtil#getPropertyValueOrDefault
     */
    public void testGetDefaultOrProperty() {
        PhpDocTag fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getPropertyValueOrDefault(fromText, "foobar"));

        fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(foobar=\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getPropertyValueOrDefault(fromText, "foobar"));

        fromText = PhpPsiElementFactory.createFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "/**\n" +
            "* @Template(test={}, car=\"\"," +
            " foobar=\"foobar.html.twig\")\n" +
            "*/\n"
        );

        assertNotNull(fromText);
        assertEquals("foobar.html.twig", AnnotationBackportUtil.getPropertyValueOrDefault(fromText, "foobar"));
    }
}
