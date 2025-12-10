package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.TwigExtensionToAttributeIntention
 */
public class TwigExtensionToAttributeIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures";
    }

    public void testIntentionIsAvailableForTwigExtension() {
        // Test for filters
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFilter;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFilter('filter_name', [$this, 'filterMethod']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function filterMethod($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n",
            "Migrate to TwigExtension attributes"
        );

        // Test for functions
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function_name', [$this, 'functionMethod']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function functionMethod($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n",
            "Migrate to TwigExtension attributes"
        );

        // Test for tests
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigTest;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getTests()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigTest('test_name', [$this, 'testMethod']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function testMethod($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n",
            "Migrate to TwigExtension attributes"
        );
    }

    public void testIntentionIsNotAvailableForNonTwigExtension() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Service;\n" +
                "\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyService\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function_name', [$this, 'functionMethod']),\n" +
                "        ];\n" +
                "    }\n" +
                "}\n"
        );

        assertFalse(
            myFixture.filterAvailableIntentions("Migrate to TwigExtension attributes")
                .stream()
                .anyMatch(action -> action.getText().equals("Migrate to TwigExtension attributes"))
        );
    }

    public void testFunctionMigrationWithMultipleOptions() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\Environment;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function_name', [$this, 'functionMethod'], [\n" +
                "                'needs_environment' => true,\n" +
                "                'needs_context' => true,\n" +
                "                'is_safe' => ['html']\n" +
                "            ]),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function functionMethod(Environment $env, $context, $value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Check that the attribute is added with all options
        assertTrue("Should have AsTwigFunction attribute", result.contains("#[AsTwigFunction('function_name', needsEnvironment: true, needsContext: true, isSafe: ['html'])]"));
        
        // Check that the import is added
        assertTrue("Should import AsTwigFunction", result.contains("use Twig\\Attribute\\AsTwigFunction;"));
        
        // Check that getFunctions method is removed
        assertFalse("Should not have getFunctions method", result.contains("public function getFunctions()"));
        
        // Check that the function method still exists
        assertTrue("Should still have functionMethod", result.contains("public function functionMethod("));
    }

    public void testMixedFiltersAndFunctions() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFilter;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFilter('filter1', [$this, 'filter1Method']),\n" +
                "            new TwigFilter('filter2', [$this, 'filter2Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function1', [$this, 'function1Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function filter1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public function filter2Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public function function1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Check that both imports are added
        assertTrue("Should import AsTwigFilter", result.contains("use Twig\\Attribute\\AsTwigFilter;"));
        assertTrue("Should import AsTwigFunction", result.contains("use Twig\\Attribute\\AsTwigFunction;"));
        
        // Check that both get methods are removed
        assertFalse("Should not have getFilters method", result.contains("public function getFilters()"));
        assertFalse("Should not have getFunctions method", result.contains("public function getFunctions()"));
        
        // Check that all attributes are added
        assertTrue("Should have AsTwigFilter for filter1", result.contains("#[AsTwigFilter('filter1')]"));
        assertTrue("Should have AsTwigFilter for filter2", result.contains("#[AsTwigFilter('filter2')]"));
        assertTrue("Should have AsTwigFunction for function1", result.contains("#[AsTwigFunction('function1')]"));
        
        // Check that all methods still exist
        assertTrue("Should still have filter1Method", result.contains("public function filter1Method("));
        assertTrue("Should still have filter2Method", result.contains("public function filter2Method("));
        assertTrue("Should still have function1Method", result.contains("public function function1Method("));
    }

    public void testPreservesExistingImports() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\Attribute\\AsTwigFilter;\n" +
                "use Twig\\TwigFilter;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFilter('filter_name', [$this, 'filterMethod']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function filterMethod($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Should not duplicate the import
        assertEquals("Should not duplicate import", 1, countOccurrences(result, "use Twig\\Attribute\\AsTwigFilter;"));
    }

    public void testExtendsRemovalAfterCompleteMigration() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFilter;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFilter('filter1', [$this, 'filter1Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function1', [$this, 'function1Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function filter1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public function function1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Check that both imports are added
        assertTrue("Should import AsTwigFilter", result.contains("use Twig\\Attribute\\AsTwigFilter;"));
        assertTrue("Should import AsTwigFunction", result.contains("use Twig\\Attribute\\AsTwigFunction;"));

        // Check that both get methods are removed
        assertFalse("Should not have getFilters method", result.contains("public function getFilters()"));
        assertFalse("Should not have getFunctions method", result.contains("public function getFunctions()"));

        // Check that the extends clause is removed
        assertFalse("Should not extend AbstractExtension", result.contains("extends AbstractExtension"));

        // Check that the AbstractExtension import is removed
        assertFalse("Should not import AbstractExtension", result.contains("use Twig\\Extension\\AbstractExtension;"));

        // Check that all attributes are added
        assertTrue("Should have AsTwigFilter for filter1", result.contains("#[AsTwigFilter('filter1')]"));
        assertTrue("Should have AsTwigFunction for function1", result.contains("#[AsTwigFunction('function1')]"));

        // Check that all methods still exist
        assertTrue("Should still have filter1Method", result.contains("public function filter1Method("));
        assertTrue("Should still have function1Method", result.contains("public function function1Method("));
    }

    public void testExtendsNotRemovedWhenGetMethodsRemain() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFilter;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFilters()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFilter('filter1', [$this, 'filter1Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('function1', [$this, 'function1Method']),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function getTests()\n" +
                "    {\n" +
                "        return [\n" +
                "            // This will be left alone, so extends should not be removed\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function filter1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public function function1Method($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention - should only migrate filters and functions
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Check that the extends clause is NOT removed because getTests still exists
        assertTrue("Should still extend AbstractExtension", result.contains("extends AbstractExtension"));

        // Check that the AbstractExtension import is NOT removed
        assertTrue("Should still import AbstractExtension", result.contains("use Twig\\Extension\\AbstractExtension;"));
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    public void testCallableReferencingOtherClassIsNotMigrated() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('foobar', [TwigExtensionRuntime::class, 'getFoobar'], [\n" +
                "                'is_safe' => ['html'],\n" +
                "            ]),\n" +
                "        ];\n" +
                "    }\n" +
                "}\n"
        );

        String originalContent = myFixture.getFile().getText();

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        // Verify the callable referencing another class was NOT migrated
        // The getFunctions method should still exist because nothing was migrated
        assertTrue("Should still have getFunctions method", result.contains("public function getFunctions()"));

        // The file should be unchanged
        assertEquals("File should not be modified when callable references another class", originalContent, result);
    }

    public void testComplexOptionValues() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\TwigFunction;\n" +
                "use Twig\\DeprecatedCallableInfo;\n" +
                "use Twig\\Node\\Node;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFunctions()\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('with_safe_callback', [$this, 'withSafeCallback'], ['is_safe_callback' => [self::class, 'checkSafeCallback']]),\n" +
                "            new TwigFunction('with_deprecation_info', [$this, 'withDeprecationInfo'], ['deprecation_info' => new DeprecatedCallableInfo('package', 'version')]),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function withSafeCallback($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public function withDeprecationInfo($value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "\n" +
                "    public static function checkSafeCallback(Node $argsNode): array\n" +
                "    {\n" +
                "        return [];\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        assertTrue("Should have isSafeCallback with class reference array", result.contains("isSafeCallback: [self::class, 'checkSafeCallback']"));
        assertTrue("Should have deprecationInfo with object instantiation", result.contains("deprecationInfo: new DeprecatedCallableInfo('package', 'version')"));

        assertTrue("Should have AsTwigFunction attribute for with_safe_callback", result.contains("#[AsTwigFunction('with_safe_callback'"));
        assertTrue("Should have AsTwigFunction attribute for with_deprecation_info", result.contains("#[AsTwigFunction('with_deprecation_info'"));

        assertTrue("Should import AsTwigFunction", result.contains("use Twig\\Attribute\\AsTwigFunction;"));

        assertFalse("Should not have getFunctions method", result.contains("public function getFunctions()"));

        assertTrue("Should still have withSafeCallback", result.contains("public function withSafeCallback("));
        assertTrue("Should still have withDeprecationInfo", result.contains("public function withDeprecationInfo("));
        assertTrue("Should still have checkSafeCallback", result.contains("public static function checkSafeCallback("));
    }

    public void testFirstClassCallableSyntax() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Twig;\n" +
                "use Twig\\Extension\\AbstractExtension;\n" +
                "use Twig\\Environment;\n" +
                "use Twig\\TwigFunction;\n" +
                "\n" +
                "class <caret>MyExtension extends AbstractExtension\n" +
                "{\n" +
                "    public function getFunctions(): array\n" +
                "    {\n" +
                "        return [\n" +
                "            new TwigFunction('with_environment', $this->withEnvironment(...), ['needs_environment' => true]),\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "    public function withEnvironment(Environment $env, $value)\n" +
                "    {\n" +
                "        return $value;\n" +
                "    }\n" +
                "}\n"
        );

        // Apply the intention
        var intention = myFixture.findSingleIntention("Migrate to TwigExtension attributes");
        myFixture.launchAction(intention);

        String result = myFixture.getFile().getText();

        assertTrue("Should have AsTwigFunction attribute", result.contains("#[AsTwigFunction('with_environment', needsEnvironment: true)]"));
        assertTrue("Should import AsTwigFunction", result.contains("use Twig\\Attribute\\AsTwigFunction;"));
        assertFalse("Should not have getFunctions method", result.contains("public function getFunctions()"));
        assertTrue("Should still have withEnvironment", result.contains("public function withEnvironment("));
    }
}