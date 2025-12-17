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
}