package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpDefine;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

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
}
