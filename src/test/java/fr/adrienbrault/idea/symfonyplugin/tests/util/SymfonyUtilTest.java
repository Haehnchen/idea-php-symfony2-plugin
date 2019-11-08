package fr.adrienbrault.idea.symfonyplugin.tests.util;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testKernelVersion() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '2.5.3';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "2.5.3"));
        assertTrue(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "2.5.2"));
        assertTrue(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "1.0"));
        assertFalse(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "2.6.1000"));
    }

    public void testKernelVersionWithDevSuffix() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-DEV';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "3.1.1"));
        assertTrue(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "3.2.0"));
        assertFalse(SymfonyUtil.isVersionGreaterThenEquals(getProject(), "3.3.0"));
    }

    public void testIsVersionLessThenEquals() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-DEV';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionLessThenEquals(getProject(), "3.2.0"));
        assertTrue(SymfonyUtil.isVersionLessThenEquals(getProject(), "3.2.1"));
        assertFalse(SymfonyUtil.isVersionLessThenEquals(getProject(), "3.1"));
    }

    public void testIsVersionLessThen() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionLessThen(getProject(), "3.3.0"));
        assertFalse(SymfonyUtil.isVersionLessThen(getProject(), "3.2.0"));
        assertFalse(SymfonyUtil.isVersionLessThen(getProject(), "3.1.0"));
    }

    public void testIsVersionGreaterThen() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-DEV';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThen(getProject(), "3.1.0"));
        assertFalse(SymfonyUtil.isVersionGreaterThen(getProject(), "3.2.0"));
    }

    public void testVersionSuffixStrip() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-RC1';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThen(getProject(), "3.1.0"));
    }

    public void testVersionSuffixStrip2() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-RC-111';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThen(getProject(), "3.1.0"));
    }

    public void testVersionSuffixStrip3() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '3.2.0-RC_111';" +
            "   }" +
            "}"
        );

        assertTrue(SymfonyUtil.isVersionGreaterThen(getProject(), "3.1.0"));
    }

    public void testIsVersionInvalidateNull() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = null" +
            "   }" +
            "}"
        );

        assertFalse(SymfonyUtil.isVersionGreaterThen(getProject(), "3.2.0"));
    }

    public void testIsVersionInvalidateDefaultValue() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION"+
            "   }" +
            "}"
        );

        assertFalse(SymfonyUtil.isVersionGreaterThen(getProject(), "3.2.0"));
    }
}
