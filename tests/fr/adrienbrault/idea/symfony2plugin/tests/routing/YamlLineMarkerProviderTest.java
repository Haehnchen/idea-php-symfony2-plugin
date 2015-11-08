package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

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
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testDoctrineFirstLineModelNavigation() {
        for (String s : new String[]{"foo.orm.yml", "foo.odm.yml", "foo.couchdb.yml", "foo.mongodb.yml", "foo.document.yml", "foo.ORM.YML"}) {
            assertLineMarker(
                createFile(YAMLFileType.YML, s, "Foo\\Bar:\n   foo:\n"),
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
                createFile(YAMLFileType.YML, String.format(
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
}
