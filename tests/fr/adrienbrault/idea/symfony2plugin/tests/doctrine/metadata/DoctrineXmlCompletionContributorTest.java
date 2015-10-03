package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineXmlCompletionContributor
 */
public class DoctrineXmlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testEntityNameCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity name=\"<caret>\"/></doctrine-mapping>",
            "DateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><entity name=\"<caret>\"/></doctrine-foo-mapping>>",
            "DateTime"
        );
    }

    public void testDocumentNameCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><document name=\"<caret>\"/></doctrine-mapping>",
            "DateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mongo-mapping><document name=\"<caret>\"/></doctrine-mongo-mapping>",
            "DateTime"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><document name=\"<caret>\"/></doctrine-foo-mapping>",
            "DateTime"
        );
    }

    public void testDocumentNameNonSupportedCompletion() {
        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<doctrine1-foo-mapping><document name=\"<caret>\"/></doctrine1-foo-mapping>",
            "DateTime"
        );

        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<doctrine1-mapping><document name=\"<caret>\"/></doctrine1-mapping>",
            "DateTime"
        );
    }

    public void testEntityRepositoryCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity repository-class=\"<caret>\"/></doctrine-mapping>",
            "BarRepo"
        );
    }

    public void testDocumentRepositoryCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><document repository-class=\"<caret>\"/></doctrine-foo-mapping>",
            "BarRepo"
        );
    }
}
