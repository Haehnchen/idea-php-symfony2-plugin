package fr.adrienbrault.idea.symfonyplugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.DoctrineMetadataLineMarkerProvider
 */
public class DoctrineMetadataLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/doctrine/metadata/fixtures";
    }

    public void testTargetDocumentLineMarker() {
        assertLineMarker(
            createXmlFile("<doctrine-mapping><document><reference-one target-document=\"MyDateTime\"/></document></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><entity><one-to-many target-entity=\"MyDateTime\"/></entity></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><embedded-document><reference-one target-document=\"MyDateTime\"/></embedded-document></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );

        assertLineMarker(
            createXmlFile("<doctrine-mapping><embedded><reference-one target-document=\"MyDateTime\"/></embedded></doctrine-mapping>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to class")
        );
    }

    public void testEmbeddableClassLineMarker() {
        assertLineMarker(
            createXmlFile("<doctrine-mapping><embeddable name=\"MyDateTime\"/></doctrine-mapping>"),
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
