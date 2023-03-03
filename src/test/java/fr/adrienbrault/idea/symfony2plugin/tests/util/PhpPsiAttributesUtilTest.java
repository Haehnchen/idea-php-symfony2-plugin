package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpPsiAttributesUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testGetAttributeValueByNameAsStringForDirectResolve() {
        PhpAttribute phpAttribute1 = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "#[Foobar(test: 'foobar')]" +
            "class Foo {}"
        );

        assertEquals("foobar", PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute1, "test"));

        PhpAttribute phpAttribute2 = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "class Foobar {}" +
            "\n" +
            "#[Foobar(test: Foobar::class)]\n" +
            "class Foo {}"
        );

        assertEquals("\\Foobar", PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute2, "test"));
    }

    public void testGetAttributeValueByNameAsStringForLocalResolve() {
         PhpAttribute phpAttribute1 = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "\n" +
            "#[Foobar(test: self::FOO)]\n" +
            "class Foo {\n" +
            " const FOO = 'test2';" +
            "}"
        );

        assertEquals("test2", PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute1, "test"));

        PhpAttribute phpAttribute2 = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "\n" +
            "class Foo {\n" +
            " const FOO = 'test2';\n" +
            "\n" +
            "#[Foobar(test: self::FOO)]\n" +
            " public function foo() {}\n" +
            "}"
        );

        assertEquals("test2", PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute2, "test"));

        PhpAttribute phpAttribute3 = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "\n" +
            "class Foo {\n" +
            " const FOO = 'test2';\n" +
            "\n" +
            "#[Foobar(test: static::FOO)]\n" +
            " public function foo() {}\n" +
            "}"
        );

        assertEquals("test2", PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute3, "test"));
    }

    public void testResolveForNoLocalValue() {
        PhpAttribute phpAttribute = PhpPsiElementFactory.createFromText(getProject(), PhpAttribute.class, "<?php\n" +
            "class FooBar {\n" +
            " const BAR = 'test2';\n" +
            "}\n" +
            "\n" +
            "class Foo {\n" +
            " const FOO = 'test2';\n" +
            "\n" +
            "#[Foobar(test: FooBar::BAR)]\n" +
            " public function foo() {}\n" +
            "}"
        );

        assertNull(PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute, "test"));
    }
}
