package fr.adrienbrault.idea.symfonyplugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.DoctrineXmlCompletionContributor
 */
public class DoctrineXmlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/doctrine/metadata/fixtures";
    }

    public void testEntityNameCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity name=\"<caret>\"/></doctrine-mapping>",
            "MyDateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><entity name=\"<caret>\"/></doctrine-foo-mapping>>",
            "MyDateTime"
        );
    }

    public void testDocumentNameCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><document name=\"<caret>\"/></doctrine-mapping>",
            "MyDateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mongo-mapping><document name=\"<caret>\"/></doctrine-mongo-mapping>",
            "MyDateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><document name=\"<caret>\"/></doctrine-foo-mapping>",
            "MyDateTime"
        );
    }

    public void testDocumentEmbeddedNameCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mongo-mapping><embedded-document name=\"<caret>\"/></doctrine-mongo-mapping>",
            "MyDateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><embedded name=\"<caret>\"/></doctrine-foo-mapping>",
            "MyDateTime"
        );
    }

    public void testDocumentNameNonSupportedCompletion() {
        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<doctrine1-foo-mapping><document name=\"<caret>\"/></doctrine1-foo-mapping>",
            "MyDateTime"
        );

        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<doctrine1-mapping><document name=\"<caret>\"/></doctrine1-mapping>",
            "MyDateTime"
        );
    }

    public void testEntityRepositoryCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity repository-class=\"<caret>\"/></doctrine-mapping>",
            "Foo\\Bar\\Ns\\BarRepo"
        );

        assertCompletionResultEquals(XmlFileType.INSTANCE, "" +
                "<doctrine-mapping><entity repository-class=\"Foo\\Bar\\Ns\\<caret>\"/></doctrine-mapping>",
            "<doctrine-mapping><entity repository-class=\"Foo\\Bar\\Ns\\BarRepo\"/></doctrine-mapping>"
        );
    }

    public void testDocumentRepositoryCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><document repository-class=\"<caret>\"/></doctrine-foo-mapping>",
            "Foo\\Bar\\Ns\\BarRepo"
        );

        assertCompletionResultEquals(XmlFileType.INSTANCE, "" +
            "<doctrine-foo-mapping><document repository-class=\"Foo\\Bar\\Ns\\<caret>\"/></doctrine-foo-mapping>",
            "<doctrine-foo-mapping><document repository-class=\"Foo\\Bar\\Ns\\BarRepo\"/></doctrine-foo-mapping>"
        );
    }

    public void testRelationCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><document><reference-one target-document=\"<caret>\"/></document></doctrine-mapping>",
            "MyDateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity><one-to-many target-entity=\"<caret>\"/></entity></doctrine-mapping>",
            "MyDateTime"
        );
    }

    public void testEmbeddableNameShouldCompleteClass() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><embeddable name=\"<caret>\"/></doctrine-mapping>",
            "MyDateTime"
        );
    }
}
