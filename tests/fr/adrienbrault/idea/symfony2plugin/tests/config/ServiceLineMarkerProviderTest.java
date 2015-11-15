package fr.adrienbrault.idea.symfony2plugin.tests.config;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.ServiceLineMarkerProvider
 */
public class ServiceLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("SymfonyPhpReferenceContributor.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testDoctrineModelLineMarker() {

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<doctrine-mapping>\n" +
            "    <document name=\"Foo\\Car\"/>\n" +
            "</doctrine-mapping>"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Foo{\n" +
                "    class Car{}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to model"));
    }

    public void testThatDoctrineAnnotationMetadataNotProvidesSelfLineMarker() {
        assertLineMarkerIsEmpty(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Foo{\n" +
                "    class Bar{}\n" +
                "}"
        ));
    }

    public void testDoctrineRepositoryDefinitionLineMarker() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
                "<doctrine-mapping>\n" +
                "    <document name=\"Foo\" repository-class=\"Entity\\Bar\"/>\n" +
                "</doctrine-mapping>"
        );

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Entity{\n" +
                "    class Bar{}\n" +
                "}"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Entity{\n" +
                "    class Bar{}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to metadata"));
    }

    public void testThatResourceProvidesLineMarker() {
        myFixture.copyFileToProject("BundleScopeLineMarkerProvider.php");

        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"@FooBundle/foo.php"});
            add(new String[] {"@FooBundle"});
            add(new String[] {"@FooBundle/"});
        }};

        for (String[] provider : providers) {
            myFixture.configureByText(
                XmlFileType.INSTANCE,
               String.format( "<routes><import resource=\"" + provider[0] + "\" /></routes>", provider)
            );

            PsiFile psiFile = myFixture.configureByText("foo.php", "");
            assertLineMarker(
                psiFile,
                new LineMarker.ToolTipEqualsAssert("Navigate to resource")
            );

            assertLineMarker(
                psiFile,
                new LineMarker.TargetAcceptsPattern("Navigate to resource", XmlPatterns.xmlTag().withName("import").withAttributeValue("resource", provider[0]))
            );
        }
    }
}
