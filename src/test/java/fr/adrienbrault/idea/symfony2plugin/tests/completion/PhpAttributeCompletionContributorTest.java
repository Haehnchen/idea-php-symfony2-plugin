package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * Test for PHP attribute completion
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeCompletionContributor
 */
public class PhpAttributeCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    public void testRouteAttributeCompletion() {
        // Test that the Route attribute appears in completion when the class exists
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nclass TestController {\n    #<caret>\n    public function index() { }\n}",
            "#[Route]"
        );
    }

    public void testNoCompletionOutsideClass() {
        // Test that no attributes are suggested outside of a class
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n    #<caret>\n    function test() { }\n",
            "#[Route]", "#[IsGranted]", "#[Cache]"
        );
    }

    public void testNoCompletionWithoutHash() {
        // Test that no attributes are suggested without the # character
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass TestController {\n    <caret>\n    public function index() { }\n}",
            "#[Route]", "#[IsGranted]", "#[Cache]"
        );
    }

    public void testCacheAttributeInsertionWithNamespaceAddsUseStatement() {
        // Test Cache attribute insertion with namespace - should add use import
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "class TestController {\n" +
                "    #<caret>\n" +
                "    public function index() { }\n" +
                "}"
        );
        myFixture.completeBasic();
        
        var items = myFixture.getLookupElements();
        var cacheItem = java.util.Arrays.stream(items)
            .filter(l -> "#[Cache]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);
        
        if (cacheItem != null) {
            myFixture.getLookup().setCurrentItem(cacheItem);
            myFixture.type('\n');
            
            String result = myFixture.getFile().getText();
            
            assertTrue("Result should contain use statement",  result.contains("use Symfony\\Component\\HttpKernel\\Attribute\\Cache;"));
            assertTrue("Result should contain empty parentheses", result.contains("#[Cache()]"));
        }
    }

    public void testCacheAttributeInsertionWithoutQuotes() {
        // Test that Cache attribute insertion doesn't include quotes (different from Route/IsGranted)
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "class TestController {\n" +
                "    #<caret>\n" +
                "    public function index() { }\n" +
                "}"
        );
        myFixture.completeBasic();
        
        var items = myFixture.getLookupElements();
        var cacheItem = java.util.Arrays.stream(items)
            .filter(l -> "#[Cache]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        myFixture.getLookup().setCurrentItem(cacheItem);
        myFixture.type('\n');

        String result = myFixture.getFile().getText();

        assertTrue("Result should contain Cache use statement", result.contains("use Symfony\\Component\\HttpKernel\\Attribute\\Cache;"));
        assertTrue("Result should contain empty parentheses", result.contains("#[Cache()]"));
    }

    public void testAsTwigFilterAttributeCompletionInClassEndingWithTwigExtension() {
        // Test that AsTwigFilter attribute appears when class name ends with "TwigExtension"
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyTwigExtension {\n    #<caret>\n    public function myFilter() { }\n}",
            "#[AsTwigFilter]"
        );
    }

    public void testAsTwigFunctionAttributeCompletionInClassExtendingAbstractExtension() {
        // Test that AsTwigFunction attribute appears when class extends AbstractExtension
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Twig\\Extension\\AbstractExtension;\n\nclass MyExtension extends AbstractExtension {\n    #<caret>\n    public function myFunction() { }\n}",
            "#[AsTwigFunction]"
        );
    }

    public void testAsTwigTestAttributeCompletionInClassWithExistingAsTwigAttribute() {
        // Test that AsTwigTest attribute appears when another method already has an AsTwig* attribute
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Twig\\Attribute\\AsTwigFilter;\n\nclass MyExtension {\n    #[AsTwigFilter('existing')]\n    public function existingFilter() { }\n\n    #<caret>\n    public function myTest() { }\n}",
            "#[AsTwigTest]"
        );
    }

    public void testAllAsTwigAttributesAvailableInTwigExtension() {
        // Test that all three AsTwig* attributes are available in a TwigExtension class
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyTwigExtension {\n    #<caret>\n    public function myMethod() { }\n}",
            "#[AsTwigFilter]", "#[AsTwigFunction]", "#[AsTwigTest]"
        );
    }

    public void testNoAsTwigAttributesInNonTwigExtensionClass() {
        // Test that AsTwig* attributes don't appear in classes that don't match TwigExtension criteria
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyService {\n    #<caret>\n    public function myMethod() { }\n}",
            "#[AsTwigFilter]", "#[AsTwigFunction]", "#[AsTwigTest]"
        );
    }

    public void testAsTwigFilterAttributeInsertionWithNamespaceAddsUseStatement() {
        // Test AsTwigFilter attribute insertion with namespace - should add use import
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Twig;\n\n" +
                "class MyTwigExtension {\n" +
                "    #<caret>\n" +
                "    public function myFilter() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var filterItem = java.util.Arrays.stream(items)
            .filter(l -> "#[AsTwigFilter]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (filterItem != null) {
            myFixture.getLookup().setCurrentItem(filterItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain use statement", result.contains("use Twig\\Attribute\\AsTwigFilter;"));
            assertTrue("Result should contain quotes for filter name", result.contains("#[AsTwigFilter(\"\")]"));
        }
    }

    public void testAsTwigFunctionAttributeInsertionWithQuotes() {
        // Test that AsTwigFunction attribute insertion includes quotes (for the Twig function name)
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Twig;\n\n" +
                "use Twig\\Extension\\AbstractExtension;\n\n" +
                "class MyExtension extends AbstractExtension {\n" +
                "    #<caret>\n" +
                "    public function myFunction() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var functionItem = java.util.Arrays.stream(items)
            .filter(l -> "#[AsTwigFunction]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (functionItem != null) {
            myFixture.getLookup().setCurrentItem(functionItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain use statement", result.contains("use Twig\\Attribute\\AsTwigFunction;"));
            assertTrue("Result should contain quotes for function name", result.contains("#[AsTwigFunction(\"\")]"));
        }
    }

    public void testNoAsTwigAttributesOutsidePublicMethod() {
        // Test that AsTwig* attributes are not suggested outside of a method
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyTwigExtension {\n    #<caret>\n    private function privateMethod() { }\n}",
            "#[AsTwigFilter]", "#[AsTwigFunction]", "#[AsTwigTest]"
        );
    }
}