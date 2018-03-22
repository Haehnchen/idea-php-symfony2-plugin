package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.XmlLineMarkerProvider
 */
public class XmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("BundleScopeLineMarkerProvider.php");
        myFixture.copyFileToProject("XmlLineMarkerProvider.php");

        myFixture.configureByText(
            XmlFileType.INSTANCE,
            "<routes><import resource=\"@FooBundle/foo.xml\" /></routes>"
        );
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatResourceProvidesLineMarkerLineMarker() {
        PsiFile psiFile = myFixture.configureByText("foo.xml", "<foo/>");
        assertLineMarker(
            psiFile,
            new LineMarker.ToolTipEqualsAssert("Navigate to resource")
        );

        assertLineMarker(
            psiFile,
            new LineMarker.TargetAcceptsPattern("Navigate to resource", XmlPatterns.xmlTag().withName("import").withAttributeValue("resource", "@FooBundle/foo.xml"))
        );
    }

    public void testThatRouteLineMarkerForControllerIsGiven() {
        assertLineMarker(
            myFixture.configureByText("foo.xml", "<routes><route controller=\"Foo\\Bar\"/></routes>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );

        assertLineMarker(
            myFixture.configureByText("foo.xml", "<routes><route><default key=\"_controller\">Foo\\Bar</default></route></routes>"),
            new LineMarker.ToolTipEqualsAssert("Navigate to action")
        );
    }
}
