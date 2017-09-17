package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.YamlLineMarkerProvider
 */
public class YamlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("YamlLineMarkerProvider.php");

        myFixture.copyFileToProject("BundleScopeLineMarkerProvider.php");
        myFixture.configureByText(
            XmlFileType.INSTANCE,
            "<routes><import resource=\"@FooBundle/foo.yml\" /></routes>"
        );
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testDoctrineFirstLineModelNavigation() {
        for (String s : new String[]{"foo.orm.yml", "foo.odm.yml", "foo.couchdb.yml", "foo.mongodb.yml", "foo.document.yml", "foo.ORM.YML"}) {
            assertLineMarker(
                myFixture.configureByText(s, "Foo\\Bar:\n   foo:\n"),
                new LineMarker.ToolTipEqualsAssert("Navigate to class")
            );
        }
    }

    public void testDoctrineMetadataRelation() {

        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"targetEntity", "Foo\\Apple"});
            add(new String[] {"targetEntity", "\\Foo\\Apple"});
            add(new String[] {"targetEntity", "Apple"});
            add(new String[] {"targetEntity", "'Apple'"});
            add(new String[] {"targetEntity", "\"Apple\""});
            add(new String[] {"targetDocument", "Apple"});
            add(new String[] {"targetDocument", "Car"});
        }};

        for (String[] provider : providers) {
            assertLineMarker(
                myFixture.configureByText(YAMLFileType.YML, String.format(
                    "Foo\\Bar:\n" +
                    "  manyToOne:\n" +
                    "    foo:\n" +
                    "      %s: %s\n"
                    , provider[0], provider[1]
                    )
                ),
                new LineMarker.ToolTipEqualsAssert("Navigate to file")
            );
        }

    }

    public void testThatResourceProvidesLineMarkerLineMarker() {
        PsiFile psiFile = myFixture.configureByText("foo.yml", "");
        assertLineMarker(
            psiFile,
            new LineMarker.ToolTipEqualsAssert("Navigate to resource")
        );

        assertLineMarker(
            psiFile,
            new LineMarker.TargetAcceptsPattern("Navigate to resource", XmlPatterns.xmlTag().withName("import").withAttributeValue("resource", "@FooBundle/foo.yml"))
        );
    }

    public void testRouteControllerActionProvidesLineMarker() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo {" +
            "   class BarController{" +
            "       function fooBarAction() {}" +
            "   }" +
            "}"
        );

        assertLineMarker(myFixture.configureByText(YAMLFileType.YML, "" +
                "foo:\n" +
                "    defaults: { _controller: Foo\\BarController::fooBarAction }\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );

        assertLineMarker(myFixture.configureByText(YAMLFileType.YML, "" +
                "foo:\n" +
                "    defaults:\n" +
                "      _controller: Foo\\BarController::fooBarAction\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );

        assertLineMarker(myFixture.configureByText(YAMLFileType.YML, "" +
                "foo:\n" +
                "    controller: Foo\\BarController::fooBarAction\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );
    }
}
