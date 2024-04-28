package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil
 */
public class PhpIndexUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpIndexUtilTest.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    public void testGetPhpClassInsideNamespace()
    {
        List<String> foobar = PhpIndexUtil.getPhpClassInsideNamespace(getProject(), "\\Foobar").stream()
            .map(PhpNamedElement::getFQN)
            .collect(Collectors.toList());

        assertContainsElements(
            foobar,
            "\\Foobar\\Class1",
            "\\Foobar\\Class2",
            "\\Foobar\\Foobar2\\Foobar3\\Class1",
            "\\Foobar\\Foobar2\\Foobar3\\Class2",
            "\\Foobar\\Foobar2\\Foobar3\\Interface1",
            "\\Foobar\\Foobar2\\Foobar3\\Interface2",
            "\\Foobar\\Foobar2\\FoobarNot\\Class1",
            "\\Foobar\\Foobar2\\FoobarNot\\Class2",
            "\\Foobar\\Foobar2\\Foobar\\Foobar4\\Class1",
            "\\Foobar\\Foobar2\\Foobar\\Foobar4\\Class2",
            "\\Foobar\\Interface1",
            "\\Foobar\\Interface2"
        );

        List<String> foobar2 = PhpIndexUtil.getPhpClassInsideNamespace(getProject(), "\\Foobar\\Foobar2\\Foobar\\Foobar4\\").stream()
            .map(PhpNamedElement::getFQN)
            .collect(Collectors.toList());

        assertContainsElements(
            foobar2,
            "\\Foobar\\Foobar2\\Foobar\\Foobar4\\Class1",
            "\\Foobar\\Foobar2\\Foobar\\Foobar4\\Class2"
        );
    }

    public void testHasNamespace()
    {
        assertTrue(PhpIndexUtil.hasNamespace(getProject(), "\\Foobar"));
        assertTrue(PhpIndexUtil.hasNamespace(getProject(), "Foobar"));
        assertFalse(PhpIndexUtil.hasNamespace(getProject(), "\\Unknown"));
        assertFalse(PhpIndexUtil.hasNamespace(getProject(), "Unknown"));
    }
}
