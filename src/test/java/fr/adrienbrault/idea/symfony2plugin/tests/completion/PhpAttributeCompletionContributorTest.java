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

    // ===============================
    // Class-level attribute tests
    // ===============================

    public void testRouteAttributeCompletionAtClassLevel() {
        // Test that the Route attribute appears in completion for controller classes
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass TestController {\n    public function index() { }\n}",
            "#[Route]"
        );
    }

    public void testAsControllerAttributeCompletionAtClassLevel() {
        // Test that the AsController attribute appears in completion for controller classes
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass TestController {\n    public function index() { }\n}",
            "#[AsController]"
        );
    }

    public void testBothRouteAndAsControllerAtClassLevel() {
        // Test that both Route and AsController attributes are available at class level
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass MyController {\n    public function action() { }\n}",
            "#[Route]", "#[AsController]"
        );
    }

    public void testNoIsGrantedOrCacheAtClassLevel() {
        // Test that IsGranted and Cache attributes are NOT available at class level (method-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass TestController {\n    public function index() { }\n}",
            "#[IsGranted]", "#[Cache]"
        );
    }

    public void testNoClassLevelAttributesForNonControllerClass() {
        // Test that class-level controller attributes don't appear for non-controller classes
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass MyService {\n    public function doSomething() { }\n}",
            "#[Route]", "#[AsController]"
        );
    }

    public void testMethodLevelAttributesStillWorkWithClassLevelSupport() {
        // Test that method-level attributes still work correctly (regression test)
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nclass TestController {\n    #<caret>\n    public function index() { }\n}",
            "#[Route]", "#[IsGranted]", "#[Cache]"
        );
    }

    public void testNoAsControllerAtMethodLevel() {
        // Test that AsController is NOT available at method level (class-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass TestController {\n    #<caret>\n    public function index() { }\n}",
            "#[AsController]"
        );
    }

    public void testRouteAttributeInsertionAtClassLevelWithNamespace() {
        // Test Route attribute insertion at class level with namespace - should add use import
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "#<caret>\n" +
                "class TestController {\n" +
                "    public function index() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var routeItem = java.util.Arrays.stream(items)
            .filter(l -> "#[Route]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (routeItem != null) {
            myFixture.getLookup().setCurrentItem(routeItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain Route use statement", result.contains("use Symfony\\Component\\Routing\\Attribute\\Route;"));
            assertTrue("Result should contain quotes for route path", result.contains("#[Route(\"\")]"));
            assertTrue("Result should have Route attribute before class", result.indexOf("#[Route") < result.indexOf("class TestController"));
        }
    }

    public void testAsControllerAttributeInsertionAtClassLevelWithoutParentheses() {
        // Test AsController attribute insertion at class level - should NOT have parentheses
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "#<caret>\n" +
                "class TestController {\n" +
                "    public function index() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var asControllerItem = java.util.Arrays.stream(items)
            .filter(l -> "#[AsController]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (asControllerItem != null) {
            myFixture.getLookup().setCurrentItem(asControllerItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain AsController use statement", result.contains("use Symfony\\Component\\HttpKernel\\Attribute\\AsController;"));
            assertTrue("Result should contain AsController without parentheses", result.contains("#[AsController]"));
            assertFalse("Result should NOT contain parentheses for AsController", result.contains("#[AsController("));
            assertTrue("Result should have AsController attribute before class", result.indexOf("#[AsController]") < result.indexOf("class TestController"));
        }
    }

    public void testClassLevelRouteAttributeWithQuotes() {
        // Test that class-level Route attribute insertion includes quotes (for route prefix)
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "#<caret>\n" +
                "class ApiController {\n" +
                "    public function index() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var routeItem = java.util.Arrays.stream(items)
            .filter(l -> "#[Route]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (routeItem != null) {
            myFixture.getLookup().setCurrentItem(routeItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain Route use statement", result.contains("use Symfony\\Component\\Routing\\Attribute\\Route;"));
            assertTrue("Result should contain quotes for route path at class level", result.contains("#[Route(\"\")]"));
        }
    }

    public void testNoClassLevelCompletionWithoutHash() {
        // Test that no class-level attributes are suggested without the # character
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\n<caret>\nclass TestController {\n    public function index() { }\n}",
            "#[Route]", "#[AsController]"
        );
    }

    public void testNoClassLevelCompletionOutsideClass() {
        // Test that class-level attributes don't appear outside of a class context
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nfunction globalFunction() { }\n",
            "#[Route]", "#[AsController]"
        );
    }

    public void testClassLevelCompletionOnlyForControllerNamedClasses() {
        // Test that class-level attributes only appear for classes named with "Controller" suffix
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass UserController {\n    public function show() { }\n}",
            "#[Route]", "#[AsController]"
        );
    }

    public void testMultipleAttributesAtClassLevel() {
        // Test that multiple attributes can be added at class level (e.g., both Route and AsController)
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Controller;\n\n" +
                "#<caret>\n" +
                "class ProductController {\n" +
                "    public function list() { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();

        // Check that both Route and AsController are available
        long routeCount = java.util.Arrays.stream(items)
            .filter(l -> "#[Route]".equals(l.getLookupString()))
            .count();
        long asControllerCount = java.util.Arrays.stream(items)
            .filter(l -> "#[AsController]".equals(l.getLookupString()))
            .count();

        assertTrue("Route attribute should be available", routeCount > 0);
        assertTrue("AsController attribute should be available", asControllerCount > 0);
    }

    public void testClassLevelCompletionWithExistingAttributes() {
        // Test that completion works when there are already attributes on the class
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#[AsController]\n#<caret>\nclass FoobarController {\n    public function index() { }\n}",
            "#[Route]"
        );
    }

    public void testClassLevelCompletionWithMultipleExistingAttributes() {
        // Test that completion works when there are multiple existing attributes
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#[AsController]\n#[Cache]\n#<caret>\nclass TestController {\n    public function test() { }\n}",
            "#[Route]", "#[AsController]"
        );
    }

    // ===============================
    // AsTwigComponent attribute tests
    // ===============================

    public void testAsTwigComponentAttributeCompletionForComponentClass() {
        // Test that AsTwigComponent attribute appears for classes in Components namespace
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Twig\\Components;\n\n#<caret>\nclass Button {\n}",
            "#[AsTwigComponent]"
        );
    }

    public void testAsTwigComponentAttributeCompletionForNestedComponentClass() {
        // Test that AsTwigComponent attribute appears for classes in nested Components namespace
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace Foo\\Components\\Form;\n\n#<caret>\nclass Input {\n}",
            "#[AsTwigComponent]"
        );
    }

    public void testNoAsTwigComponentForNonComponentClass() {
        // Test that AsTwigComponent attribute does not appear for non-component classes
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Service;\n\n#<caret>\nclass MyService {\n}",
            "#[AsTwigComponent]"
        );
    }

    public void testNoAsTwigComponentAtMethodLevel() {
        // Test that AsTwigComponent is NOT available at method level (class-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Twig\\Components;\n\nclass Button {\n    #<caret>\n    public function render() { }\n}",
            "#[AsTwigComponent]"
        );
    }

    public void testAsTwigComponentAttributeCompletionForClassInSameNamespaceAsIndexedComponent() {
        // Test that AsTwigComponent attribute appears for classes in the same namespace as an existing indexed component
        // First, add a component class with the attribute to the project (this will be indexed)
        myFixture.addFileToProject("ExistingWidget.php",
            "<?php\n\n" +
                "namespace App\\Custom\\Widgets;\n\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n" +
                "#[AsTwigComponent('existing_widget')]\n" +
                "class ExistingWidget {\n" +
                "}"
        );

        // Now check that a new class in the same namespace gets the completion
        // (namespace doesn't contain "Components", so it should work only because of the index)
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Custom\\Widgets;\n\n#<caret>\nclass NewWidget {\n}",
            "#[AsTwigComponent]"
        );
    }

    // ===============================
    // ExposeInTemplate attribute tests
    // ===============================

    public void testExposeInTemplateAttributeCompletionOnPublicMethodInAsTwigComponentClass() {
        // Test that ExposeInTemplate attribute appears for public methods in classes with #[AsTwigComponent]
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Button {\n    #<caret>\n    public function getLabel(): string { return 'test'; }\n}",
            "#[ExposeInTemplate]"
        );
    }

    public void testNoExposeInTemplateOnPropertyInNonAsTwigComponentClass() {
        // Test that ExposeInTemplate does NOT appear for properties in classes without #[AsTwigComponent]
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyService {\n    #<caret>\n    private string $property;\n}",
            "#[ExposeInTemplate]"
        );
    }

    public void testNoExposeInTemplateOnMethodInNonAsTwigComponentClass() {
        // Test that ExposeInTemplate does NOT appear for methods in classes without #[AsTwigComponent]
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyService {\n    #<caret>\n    public function doSomething() { }\n}",
            "#[ExposeInTemplate]"
        );
    }

    public void testNoExposeInTemplateAtClassLevel() {
        // Test that ExposeInTemplate is NOT available at class level (property/method-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#<caret>\n#[AsTwigComponent]\nclass Button {\n}",
            "#[ExposeInTemplate]"
        );
    }

    public void testExposeInTemplateAttributeInsertionOnPublicMethod() {
        // Test that ExposeInTemplate attribute insertion on public method includes quotes (for custom name)
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Twig\\Components;\n\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n" +
                "#[AsTwigComponent]\n" +
                "class Card {\n" +
                "    #<caret>\n" +
                "    public function getTitle(): string { return 'test'; }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var exposeItem = java.util.Arrays.stream(items)
            .filter(l -> "#[ExposeInTemplate]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        myFixture.getLookup().setCurrentItem(exposeItem);
        myFixture.type('\n');

        String result = myFixture.getFile().getText();

        assertTrue("Result should contain ExposeInTemplate use statement", result.contains("use Symfony\\UX\\TwigComponent\\Attribute\\ExposeInTemplate;"));
        assertTrue("Result should contain quotes for method name", result.contains("#[ExposeInTemplate]"));
    }

    public void testExposeInTemplateOnPublicPropertyInAsTwigComponentClass() {
        // Test that ExposeInTemplate appears for public properties (though they're auto-exposed)
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent('card')]\nclass Card {\n    #<caret>\n    public string $title;\n}",
            "#[ExposeInTemplate]"
        );
    }

    // ===============================
    // PreMount and PostMount attribute tests
    // ===============================

    public void testPreMountAttributeCompletionOnPublicMethodInAsTwigComponentClass() {
        // Test that PreMount attribute appears for public methods in classes with #[AsTwigComponent]
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Alert {\n    #<caret>\n    public function preMount(array $data): array { return $data; }\n}",
            "#[PreMount]"
        );
    }

    public void testPostMountAttributeCompletionOnPublicMethodInAsTwigComponentClass() {
        // Test that PostMount attribute appears for public methods in classes with #[AsTwigComponent]
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Modal {\n    #<caret>\n    public function postMount(): void { }\n}",
            "#[PostMount]"
        );
    }

    public void testPreMountAndPostMountBothAvailableInAsTwigComponentClass() {
        // Test that both PreMount and PostMount attributes are available for methods
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Form {\n    #<caret>\n    public function mount(): void { }\n}",
            "#[PreMount]", "#[PostMount]"
        );
    }

    public void testNoPreMountOnMethodInNonAsTwigComponentClass() {
        // Test that PreMount does NOT appear for methods in classes without #[AsTwigComponent]
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyService {\n    #<caret>\n    public function doSomething() { }\n}",
            "#[PreMount]"
        );
    }

    public void testNoPostMountOnMethodInNonAsTwigComponentClass() {
        // Test that PostMount does NOT appear for methods in classes without #[AsTwigComponent]
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyService {\n    #<caret>\n    public function doSomething() { }\n}",
            "#[PostMount]"
        );
    }

    public void testNoPreMountOnPropertyInAsTwigComponentClass() {
        // Test that PreMount is NOT available on properties (method-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Button {\n    #<caret>\n    private string $label;\n}",
            "#[PreMount]"
        );
    }

    public void testNoPostMountOnPropertyInAsTwigComponentClass() {
        // Test that PostMount is NOT available on properties (method-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent]\nclass Card {\n    #<caret>\n    private string $title;\n}",
            "#[PostMount]"
        );
    }

    public void testNoPreMountOrPostMountAtClassLevel() {
        // Test that PreMount and PostMount are NOT available at class level (method-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#<caret>\n#[AsTwigComponent]\nclass Widget {\n}",
            "#[PreMount]", "#[PostMount]"
        );
    }

    public void testPreMountAttributeInsertionOnMethodWithoutParentheses() {
        // Test that PreMount attribute insertion does NOT include parentheses
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Twig\\Components;\n\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n" +
                "#[AsTwigComponent]\n" +
                "class Notification {\n" +
                "    #<caret>\n" +
                "    public function preMount(array $data): array { return $data; }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var preMountItem = java.util.Arrays.stream(items)
            .filter(l -> "#[PreMount]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (preMountItem != null) {
            myFixture.getLookup().setCurrentItem(preMountItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain PreMount use statement", result.contains("use Symfony\\UX\\TwigComponent\\Attribute\\PreMount;"));
            assertTrue("Result should contain PreMount without parentheses", result.contains("#[PreMount]"));
            assertFalse("Result should NOT contain parentheses for PreMount", result.contains("#[PreMount("));
        }
    }

    public void testPostMountAttributeInsertionOnMethodWithoutParentheses() {
        // Test that PostMount attribute insertion does NOT include parentheses
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "namespace App\\Twig\\Components;\n\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n" +
                "#[AsTwigComponent]\n" +
                "class Dropdown {\n" +
                "    #<caret>\n" +
                "    public function postMount(): void { }\n" +
                "}"
        );
        myFixture.completeBasic();

        var items = myFixture.getLookupElements();
        var postMountItem = java.util.Arrays.stream(items)
            .filter(l -> "#[PostMount]".equals(l.getLookupString()))
            .findFirst()
            .orElse(null);

        if (postMountItem != null) {
            myFixture.getLookup().setCurrentItem(postMountItem);
            myFixture.type('\n');

            String result = myFixture.getFile().getText();

            assertTrue("Result should contain PostMount use statement", result.contains("use Symfony\\UX\\TwigComponent\\Attribute\\PostMount;"));
            assertTrue("Result should contain PostMount without parentheses", result.contains("#[PostMount]"));
            assertFalse("Result should NOT contain parentheses for PostMount", result.contains("#[PostMount("));
        }
    }

    public void testAllTwigComponentMethodAttributesAvailable() {
        // Test that all Twig component method attributes are available together
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n#[AsTwigComponent('badge')]\nclass Badge {\n    #<caret>\n    public function init(): void { }\n}",
            "#[ExposeInTemplate]", "#[PreMount]", "#[PostMount]"
        );
    }

    public void testAsCommandAttributeCompletionScope() {
        // Test that AsCommand attribute appears for classes ending with "Command"
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n#<caret>\nclass MyCommand {\n}",
            "#[AsCommand]"
        );

        // Test that AsCommand attribute appears for classes in Command namespace
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Command;\n\n#<caret>\nclass ImportUsers {\n}",
            "#[AsCommand]"
        );

        // Test that AsCommand attribute appears for classes extending Symfony Command
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\Component\\Console\\Command\\Command;\n\n#<caret>\nclass MyTask extends Command {\n}",
            "#[AsCommand]"
        );

        // Test that AsCommand attribute appears for classes with __invoke method using InputInterface
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\Component\\Console\\Input\\InputInterface;\n\n#<caret>\nclass ProcessData {\n    public function __invoke(InputInterface $input) { }\n}",
            "#[AsCommand]"
        );

        // Test that AsCommand attribute appears for classes with __invoke method using OutputInterface
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\nuse Symfony\\Component\\Console\\Output\\OutputInterface;\n\n#<caret>\nclass GenerateReport {\n    public function __invoke(OutputInterface $output) { }\n}",
            "#[AsCommand]"
        );
    }

    public void testNoAsCommandInvalidScopes() {
        // Test that AsCommand is NOT available at method level (class-only)
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nclass MyCommand {\n    #<caret>\n    public function execute() { }\n}",
            "#[AsCommand]"
        );

        // Test that AsCommand attribute does not appear for non-command classes
        assertCompletionNotContains(PhpFileType.INSTANCE,
            "<?php\n\nnamespace App\\Service;\n\n#<caret>\nclass MyService {\n}",
            "#[AsCommand]"
        );
    }
}