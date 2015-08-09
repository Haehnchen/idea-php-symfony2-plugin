package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml.doctrine;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineOrmYamlTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("DoctrineTypes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor
     */
    public void testDoctrineOrmFieldCompletion() {

        assertCompletionContains("foo.orm.yml", "foo:\n" +
                "    fields:\n" +
                "        field_1:\n" +
                "            type: <caret>",
            "BAR", "foo_const", "id"
        );

        assertCompletionContains("foo.orm.yml", "foo:\n" +
                "    id:\n" +
                "        field_1:\n" +
                "            type: <caret>",
            "BAR", "foo_const", "id"
        );
    }
    
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlGoToKnownDeclarationHandler
     */
    public void testDoctrineOrmFieldNavigation() {

        assertNavigationMatch("foo.orm.yml", "foo:\n" +
                "    fields:\n" +
                "        field_1:\n" +
                "            type: BA<caret>R",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch("foo.orm.yml", "foo:\n" +
                "    fields:\n" +
                "        field_1:\n" +
                "            type: foo_c<caret>onst",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch("foo.orm.yml", "foo:\n" +
                "    id:\n" +
                "        field_1:\n" +
                "            type: BA<caret>R",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }
}
