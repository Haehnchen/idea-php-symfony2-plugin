package fr.adrienbrault.idea.symfonyplugin.tests.security;

import com.intellij.patterns.PlatformPatterns;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.security.AnnotationExpressionGotoCompletionRegistrar
 */
public class AnnotationExpressionGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("security.yml");
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/security/fixtures";
    }

    public void testSecurityAnnotationProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "" +
                "/**\n" +
                "* @Security(\"has_role('<caret>')\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "" +
                "/**\n" +
                "* @Security(\"has_role('YAML_ROLE_<caret>')\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "" +
                "/**\n" +
                "* @Security(\"is_granted('<caret>')\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "" +
                "/**\n" +
                "* @Security(\"is_granted('<caret>', foo)\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testSecurityAnnotationProvidesRoleNavigation() {
        assertNavigationMatch(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "/**\n" +
                "* @Security(\"has_role('YAML_ROLE<caret>_USER_FOOBAR')\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "/**\n" +
                "* @Security(\"has_role   (     'YAML_ROLE<caret>_USER_FOOBAR'    )   \")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "/**\n" +
                "* @Security(\"is_granted('YAML_ROLE<caret>_USER_FOOBAR')\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            "test.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security;\n" +
                "/**\n" +
                "* @Security(\"is_granted('YAML_ROLE<caret>_USER_FOOBAR', post)\")\n" +
                "*/\n" +
                "function test() {};\n" +
                "",
            PlatformPatterns.psiElement()
        );
    }
}
