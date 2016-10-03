package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataLineMarkerProvider
 */
public class DoctrineMetadataLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTargetDocumentLineMarker() {
        assertLineMarker(
            createXmlFile("<doctrine-mapping><document><reference-one target-document=\"Datetime\"/></document></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><entity><one-to-many target-entity=\"Datetime\"/></entity></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><embedded-document><reference-one target-document=\"Datetime\"/></embedded-document></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><embedded><reference-one target-document=\"Datetime\"/></embedded></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );
    }

    public void testEmbeddableClassLineMarker() {
        assertLineMarker(
            createXmlFile("<doctrine-mapping><embeddable name=\"Datetime\"/></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );
    }

    public void testTargetDocumentLineMarkerInSameNamespace() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Entity{\n" +
                "    class Bar{}\n" +
                "    class Relation{}\n" +
                "}"
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><entity name=\"Entity\\Bar\"><one-to-many target-entity=\"Relation\"/></entity></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );
    }

    @NotNull
    private PsiElement createXmlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }
}
