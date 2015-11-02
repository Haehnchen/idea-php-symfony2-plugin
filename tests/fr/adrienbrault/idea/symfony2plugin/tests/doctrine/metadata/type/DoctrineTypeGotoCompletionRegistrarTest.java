package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata.type;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type.DoctrineTypeGotoCompletionRegistrar
 */
public class DoctrineTypeGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testXmlTypes() {
        for (String s : new String[]{"couchdb", "orm", "mongodb"}) {
            assertCompletionContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"<caret>\" /></document></doctrine-mapping>",
                "string", s + "_foo_bar"
            );

            assertCompletionContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><embedded-document><field type=\"<caret>\" /></embedded-document></doctrine-mapping>",
                "string", s + "_foo_bar"
            );

            assertCompletionContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><embedded><field type=\"<caret>\" /></embedded></doctrine-mapping>",
                "string", s + "_foo_bar"
            );

            assertNavigationMatch(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"string<caret>\" /></document></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                "foo." + s + ".xml",
                "<doctrine-mapping><embedded-document><field type=\"string<caret>\" /></embedded-document></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                "foo." + s + ".xml",
                "<doctrine-mapping><embedded><field type=\"string<caret>\" /></embedded></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertCompletionNotContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"<caret>\" /></document></doctrine-mapping>",
                "foo"
            );
        }
    }

    public void testThatOrmTypeFilledByStatic() {
        assertCompletionContains(
            "foo.orm.xml",
            "<doctrine-mapping><document><field type=\"<caret>\" /></document></doctrine-mapping>",
            "string", "orm_foo_bar", "smallint", "array"
        );
    }

    public void testYamlTypes() {
        for (String s : new String[]{"couchdb", "orm", "mongodb"}) {

            assertCompletionContains("foo." + s + ".yml", "foo:\n" +
                "    id:\n" +
                "        field_1:\n" +
                "            type: <caret>",
                "string", s + "_foo_bar"
            );

            assertNavigationMatch("foo." + s + ".yml", "foo:\n" +
                "    fields:\n" +
                "        field_1:\n" +
                "            type: stri<caret>ng",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertCompletionNotContains("foo." + s + ".yml", "foo:\n" +
                    "    id:\n" +
                    "        field_1:\n" +
                    "            type: <caret>",
                "string", s + "_foo_bar",
                "foo"
            );
        }
    }

    public void testDocumentAndOdmFallback() {
        for (String s : new String[]{"document", "odm"}) {
            assertCompletionContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"<caret>\" /></document></doctrine-mapping>",
                "string", "couchdb_foo_bar"
            );

            assertNavigationMatch(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"couchdb_foo_bar<caret>\" /></document></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertCompletionContains(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"<caret>\" /></document></doctrine-mapping>",
                "string", "mongodb_foo_bar"
            );

            assertNavigationMatch(
                "foo." + s + ".xml",
                "<doctrine-mapping><document><field type=\"mongodb_foo_bar<caret>\" /></document></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

}
