package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable.collector;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.FileDocVariableCollector
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil#findFileVariableDocBlock
 */
public class FileDocVariableCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Bar;\n" +
            "class FooClass {\n" +
            "  public function getFooBar() {}\n" +
            "  public function setMyBar() {}\n" +
            "  public function isMyCar() {}\n" +
            "  public function cool() {}\n" +
            "  public function Hot() {}\n" +
            "  protected function protectedBar() {}\n" +
            "  private function privateBar() {}\n" +
            "  /** @return FooClass[] */\n" +
            "  public function getNested() {}\n" +
            "  /** @return FooClass[] */\n" +
            "  public function getIterator() {}\n" +
            "}\n" +
            "\n" +
            "namespace Foo\\Dto;\n" +
            "class FooDto {\n" +
            "  public function __construct(public FoobarDto $foobar) {}\n" +
            "}\n" +
            "class FoobarDto {\n" +
            "  /**\n" +
            "   * @param BarDto[] $bars\n" +
            "   */\n" +
            "  public function __construct(public array $bars) {}\n" +
            "}\n" +
            "class BarDto {\n" +
            "  public function getBaz() {}\n" +
            "}"
        );
    }

    public void testFileBasedVarDocPhpTypes() {
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass #} {{ <caret> }}", "bar");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass #} {{ bar.<caret> }}", "fooBar", "myCar", "cool", "Hot");

        assertCompletionNotContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass #} {{ bar.<caret> }}", "myBar");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass #} {{ bar.<caret> }}", "protectedBar");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass #} {{ bar.<caret> }}", "privateBar");
    }

    public void testFileBasedVarDocPhpTypesAsMultiline() {
        assertCompletionContains(TwigFileType.INSTANCE,  "{# @var bar \\Bar\\FooClass \n @var foo \\Bar\\FooClass #} {{ <caret> }}", "bar", "foo");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass \n @var foo \\Bar\\FooClass #} {{ bar.<caret> }}", "fooBar");
        assertCompletionContains(TwigFileType.INSTANCE, "{# @var bar \\Bar\\FooClass \n @var foo \\Bar\\FooClass #} {{ foo.<caret> }}", "fooBar");
    }

    public void testFileBasedVarDocPhpTypesWithDescription() {
        assertCompletionContains(TwigFileType.INSTANCE,  "{# @var bar \\Bar\\FooClass Foo asd Kassdsdsd adasd \n @var foo \\Bar\\FooClass #} {{ <caret> }}", "bar", "foo");
    }

    public void testFileBasedVarDocPhpTypesAsDeprecated() {
        // remove on dropped feature
        assertCompletionContains(TwigFileType.INSTANCE, "{# bar \\Bar\\FooClass #} {{ <caret> }}", "bar");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil#collectForArrayScopeVariables
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil#getForTagScope
     */
    public void testVarArrayIteration() {

        assertCompletionContains(TwigFileType.INSTANCE, "" +
            "{# @var bars \\Bar\\FooClass[] #}\n" +
            "{% for bar in bars %}\n" +
            "  {{ <caret> }}\n" +
            "{% endfor %}\n"
            , "bar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
            "{# @var bars \\Bar\\FooClass[] #}\n" +
            "{% for bar in bars %}\n" +
            "  {{ bar.<caret> }}\n" +
            "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass[] #}\n" +
                "{% for bar in bars|foo %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass[] #}\n" +
                "{% for bar in bars | foo %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );
    }

    public void testVarArrayIterationViaIterationClassImplementations() {
        myFixture.configureByText("class1.php", "<?php\n" +
            "namespace Bar;\n" +
            "/**\n" +
            " * @method FooClassIteratorArray[] __iterator\n" +
            " */\n" +
            "class FooClassIteratorArray {\n" +
            "  public function getFooBar() {}\n" +
            "}"
        );

        myFixture.configureByText("class2.php", "<?php\n" +
            "namespace Bar;\n" +
            "/**\n" +
            " * @method FooClassIterator __iterator\n" +
            " */\n" +
            "class FooClassIterator {\n" +
            "  public function getFooBar() {}\n" +
            "}"
        );

        myFixture.configureByText("class3.php", "<?php\n" +
            "namespace Bar;\n" +
            "/**\n" +
            " * @method FooClassCurrent current\n" +
            " */\n" +
            "class FooClassCurrent {\n" +
            "  public function getFooBar() {}\n" +
            "}"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass #}\n" +
                "{% for bar in bars %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClassIteratorArray #}\n" +
                "{% for bar in bars %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClassIterator #}\n" +
                "{% for bar in bars %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClassCurrent #}\n" +
                "{% for bar in bars %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );
    }

    public void testVarArrayIterationViaDoctrineArrayCollectionSubclass() {
        myFixture.configureByText("issue_1097.php", "<?php\n" +
            "namespace Doctrine\\Common\\Collections;\n" +
            "class ArrayCollection {}\n" +
            "\n" +
            "namespace AppBundle\\Entity;\n" +
            "class Provider {\n" +
            "  public function getName() {}\n" +
            "}\n" +
            "\n" +
            "namespace AppBundle\\Service;\n" +
            "use AppBundle\\Entity\\Provider;\n" +
            "use Doctrine\\Common\\Collections\\ArrayCollection;\n" +
            "/**\n" +
            " * @method Provider __iterator\n" +
            " */\n" +
            "class ProviderCollection extends ArrayCollection {\n" +
            "}"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var providers \\AppBundle\\Service\\ProviderCollection #}\n" +
                "{% for provider in providers %}\n" +
                "  {{ provider.<caret> }}\n" +
                "{% endfor %}\n"
            , "name"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil#collectForArrayScopeVariables
     */
    public void testVarChainArrayIteration() {

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass #}\n" +
                "{% for bar in bars.nested %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass #}\n" +
                "{% for bar in bars.nested | foo %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

    }

    public void testVarChainArrayIterationUsesPromotedConstructorParamArrayType() {
        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var foo \\Foo\\Dto\\FooDto #}\n" +
                "{% for bar in foo.foobar.bars %}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "baz"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil#collectForArrayScopeVariables
     */
    public void testThatDuplicateScopeVariablesAreMerged() {

        assertCompletionContains(TwigFileType.INSTANCE, "" +
                "{# @var bars \\Bar\\FooClass #}\n" +
                "{% for bar in bars.fooBar %}\n" +
                "  {# @var bar \\Bar\\FooClass #}\n" +
                "  {{ bar.<caret> }}\n" +
                "{% endfor %}\n"
            , "fooBar"
        );

    }

}
