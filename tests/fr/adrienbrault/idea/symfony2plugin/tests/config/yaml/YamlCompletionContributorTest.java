package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("config_foo.yml", "");
    }

    public void testResourcesInsideSameDirectoryProvidesCompletion() {
        assertCompletionContains("config.yml", "imports:\n" +
                "    - { resource: <caret> }",
            "config_foo.yml"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor.RepositoryClassCompletionProvider
     */
    public void testRepositoryClassCompletionProvider() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace Doctrine\\Common\\Persistence {\n" +
                "    interface ObjectRepository {};\n" +
                "}\n" +
                "\n" +
                "namespace Foo\\Bar {\n" +
                "    use Doctrine\\Common\\Persistence\\ObjectRepository;\n" +
                "    abstract class BarRepository implements ObjectRepository { }\n" +
                "}"
        );

        for (String s : new String[]{"orm.yml", "couchdb.yml", "odm.yml", "mongodb.yml"}) {
            assertCompletionContains(
                "foo." + s,
                "Foo:\n  repositoryClass: <caret>",
                "Foo\\Bar\\BarRepository"
            );

            assertCompletionResultEquals(
                "foo." + s,
                "Foo:\n  repositoryClass: Foo\\Bar\\<caret>",
                "Foo:\n  repositoryClass: Foo\\Bar\\BarRepository"
            );
        }
    }
}
