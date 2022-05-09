package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpDefine;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("YamlReferenceContributor.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/yaml/fixtures";
    }

    public void testConstantProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
                "  app.service.example:\n" +
                "    arguments:\n" +
                "      - !php/const CONST_<caret>FOO\n",
            PlatformPatterns.psiElement(PhpDefine.class).withName("CONST_FOO")
        );

        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.example:\n" +
            "    arguments:\n" +
            "      - !php/const Foo\\Bar::F<caret>OO\n",
            PlatformPatterns.psiElement(Field.class).withName("FOO")
        );
    }

    public void testArgumentsSequenceProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.bar:\n" +
            "    class: App\\BarService\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    class: App\\FooService\n" +
            "    arguments:\n"+
            "      - '@app.service.bar<caret>'\n",
            getExpectedReferencePattern("app.service.bar")
        );
    }

    public void testArgumentsMapProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.bar:\n" +
            "    class: App\\BarService\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    class: App\\FooService\n" +
            "    arguments:\n"+
            "      - '@app.service.bar<caret>'\n",
            getExpectedReferencePattern("app.service.bar")
        );
    }

    public void testPropertiesProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.bar:\n" +
            "    class: App\\BarService\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    class: App\\FooService\n" +
            "    properties:\n"+
            "      bar: '@app.service.bar<caret>'\n",
            getExpectedReferencePattern("app.service.bar")
        );
    }

    public void testAliasProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.bar:\n" +
            "    class: App\\BarService\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    alias: app.service.<caret>bar\n",
            getExpectedReferencePattern("app.service.bar")
        );
    }

    public void testAliasShortcutProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.bar:\n" +
            "    class: App\\BarService\n" +
            "\n" +
            "  app.service.foo: '@app.service.<caret>bar'\n",
            getExpectedReferencePattern("app.service.bar")
        );
    }

    public void testDecoratesProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo:\n" +
            "    class: App\\FooService\n" +
            "\n" +
            "  app.service.foo_decorator:\n" +
            "    decorates: app.service.<caret>foo\n",
            getExpectedReferencePattern("app.service.foo")
        );
    }

    public void testConfiguratorInvokableProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo_configurator: ~\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    configurator: '@app.service.foo_configurator<caret>'\n",
            getExpectedReferencePattern("app.service.foo_configurator")
        );
    }

    public void testFactoryInvokableProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo_factory: ~\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    factory: '@app.service.foo_factory<caret>'\n",
            getExpectedReferencePattern("app.service.foo_factory")
        );
    }

    public void testFactoryProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo_factory: ~\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    factory: ['@app.service.foo_factory<caret>', 'create']\n",
            getExpectedReferencePattern("app.service.foo_factory")
        );
    }

    public void testConfiguratorProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo_configurator: ~\n" +
            "\n" +
            "  app.service.foo:\n" +
            "    configurator: ['@app.service.foo_configurator<caret>', 'configure']\n",
            getExpectedReferencePattern("app.service.foo_configurator")
        );
    }

    public void testCallsArgsProvidesReferences() {
        assertReferenceMatchOnParent(
            YAMLFileType.YML,
            "services:\n" +
            "  app.service.foo: ~\n" +
            "\n" +
            "  app.service.bar:\n" +
            "    calls: " +
            "      - setFoo: ['@app.service.foo<caret>']\n",
            getExpectedReferencePattern("app.service.foo")
        );
    }

    @NotNull
    private PsiElementPattern.Capture<YAMLKeyValue> getExpectedReferencePattern(@NotNull String serviceName) {
        return PlatformPatterns.psiElement(YAMLKeyValue.class).withName(serviceName);
    }
}
