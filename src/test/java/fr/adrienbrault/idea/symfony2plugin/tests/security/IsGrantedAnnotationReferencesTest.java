package fr.adrienbrault.idea.symfony2plugin.tests.security;

import com.intellij.patterns.PlatformPatterns;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class IsGrantedAnnotationReferencesTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("security.yml");
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/security/fixtures";
    }

    public void testThatIsGrantedAnnotationProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\IsGranted;\n" +
                "" +
                "/**\n" +
                "* @IsGranted(\"<caret>\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testThatIsGrantedAnnotationProvidesRoleNavigation() {
        assertReferenceMatchOnParent(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\IsGranted;\n" +
                "/**\n" +
                "* @IsGranted(\"YAML_ROLE<caret>_USER_FOOBAR\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            PlatformPatterns.psiElement()
        );
    }
}
