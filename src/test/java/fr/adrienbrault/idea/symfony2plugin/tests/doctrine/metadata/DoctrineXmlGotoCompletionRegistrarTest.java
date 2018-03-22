package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineXmlGotoCompletionRegistrar
 */
public class DoctrineXmlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testEntityNameNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity name=\"Foo\\Bar\\Ns<caret>\\Bar\"/></doctrine-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testDocumentNameNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><document name=\"Foo\\Bar\\Ns<caret>\\Bar\"/></doctrine-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mongo-mapping><document name=\"Foo\\Bar\\Ns<caret>\\Bar\"/></doctrine-mongo-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testEntityRepositoryClassNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><entity repository-class=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></doctrine-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testDocumentRepositoryClassNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mongo-mapping><document repository-class=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></doctrine-mongo-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );

        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-foo-mapping><document repository-class=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></doctrine-foo-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    /**
     * @see DoctrineMetadataPattern#getXmlTargetDocumentClass()
     */
    public void testTargetDocumentNavigation() {
        for(String s : new String[] {"reference-one", "reference-many", "embed-many", "embed-one"}) {
            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-mapping><document><" + s + " target-document=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></document></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-foo-mapping><document><" + s + " target-document=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></document></doctrine-foo-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-foo-mapping><document name=\"Foo\\Bar\\Ns\\Foo\"><" + s + " target-document=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></document></doctrine-foo-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

    /**
     * @see DoctrineMetadataPattern#getXmlTargetEntityClass()
     */
    public void testTargetEntityNavigation() {
        for(String s : new String[] {"one-to-one", "one-to-many", "many-to-one", "many-to-many"}) {
            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-mapping><entity><" + s + " target-entity=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></entity></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-foo-mapping><entity><" + s + " target-entity=\"Foo\\Bar\\Ns<caret>\\BarRepo\"/></entity></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                XmlFileType.INSTANCE,
                "<doctrine-foo-mapping><entity name=\"Foo\\Bar\\Ns\\Foo\"><" + s + " target-entity=\"Bar<caret>Repo\"/></entity></doctrine-mapping>",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

    public void testEmbeddedNameNavigation() {
        assertNavigationMatch(
            XmlFileType.INSTANCE,
            "<doctrine-mapping><embedded name=\"Foo\\Bar\\Ns<caret>\\Bar\"/></doctrine-mapping>",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }
}
