package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;

/**
 * @see AnnotationBackportUtil#getQualifiedName
 */
public class AnnotationBackportUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testGetQualifiedNameAcceptsMissingLeadingSlash() {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace App\\EventListener;\n" +
            "class Foo {}\n"
        );

        assertEquals(
            "Symfony\\Component\\Form\\Event\\PreSubmitEvent",
            AnnotationBackportUtil.getQualifiedName(phpClass, "Symfony\\Component\\Form\\Event\\PreSubmitEvent")
        );
    }

    public void testGetQualifiedNameKeepsImportedAliasForMissingLeadingSlash() {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace App\\EventListener;\n" +
            "use Symfony\\Component\\Form\\Event\\PreSubmitEvent;\n" +
            "class Foo {}\n"
        );

        assertEquals(
            "PreSubmitEvent",
            AnnotationBackportUtil.getQualifiedName(phpClass, "Symfony\\Component\\Form\\Event\\PreSubmitEvent")
        );
    }
}
