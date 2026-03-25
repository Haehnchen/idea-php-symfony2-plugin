package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.linemarker.XmlLineMarkerProvider
 */
public class XmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/linemarker/fixtures";
    }

    public void testThatDecoratedServiceShouldProvideMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" class=\"Foo\\Bar\\Apple\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decoration")
        );
    }

    public void testThatParentServiceShouldProvideMarker() {
        myFixture.addFileToProject("config/services_parent.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "       <service id=\"app.child\" parent=\"foo_bar_parent_main_xml\"/>\n" +
            "    </services>\n" +
            "</container>");

        PsiElement xmlFile = createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "       <service id=\"foo_bar_parent_main_xml\" class=\"Foo\\Bar\\Apple\"/>\n" +
            "    </services>\n" +
            "</container>"
        );

        assertLineMarker(xmlFile, new LineMarker.ToolTipEqualsAssert("Navigate to parent"));
        assertLineMarker(xmlFile, new LineMarker.TargetAcceptsPattern("Navigate to parent",
            XmlPatterns.xmlTag().withName("service").withAttributeValue("id", "app.child"))
        );
    }

    public void testThatDecoratesProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" decorates=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decorated service")
        );
    }

    public void testThatParentProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" parent=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to parent service")
        );
    }

    public void testThatPrototypeNamespaceResourceIsHavingLinemarker() {
        myFixture.addFileToProject("src/Controller/FooController.php", "<?php\n" +
            "namespace App\\Controller;\n" +
            "class FooController {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.xml",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "        <prototype namespace=\"App\\Controller\\\" resource=\"../src/Controller/*\"/>\n" +
            "    </services>\n" +
            "</container>");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(myFixture.getFile(), new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(@NotNull PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Controller\\FooController".equals(phpClass.getFQN());
                }
            }))
        );
    }

    @NotNull
    private PsiElement createXmlFile(@NotNull String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }
}
