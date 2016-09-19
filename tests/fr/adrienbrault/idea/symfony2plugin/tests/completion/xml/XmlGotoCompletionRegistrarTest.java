package fr.adrienbrault.idea.symfony2plugin.tests.completion.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.completion.xml.XmlGotoCompletionRegistrar
 */
public class XmlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureByText("config_foo.xml", "<foo/>");
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("XmlGotoCompletionRegistrar.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testResourcesInsideSameDirectoryProvidesNavigation() {
        assertNavigationContainsFile(
            XmlFileType.INSTANCE,
            "<import resource=\"config<caret>_foo.xml\"/>",
            "config_foo.xml"
        );
    }

    public void testIdInsideServiceTagShouldCompleteWithClassName() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<services><service id=\"<caret>\" class=\"MyFoo\\Foo\\Apple\"/></services>",
            "my_foo.foo.apple"
        );

        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<service id=\"<caret>\" class=\"MyFoo\\Foo\\Apple\"/>",
            "my_foo.foo.apple"
        );

        assertCompletionNotContains(
            XmlFileType.INSTANCE,
            "<service id=\"<caret>\"/>",
            "my_foo.foo.apple"
        );
    }

    public void testThatServiceFactoryMethodAttributeProvidesCompletion() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <factory service=\"foo.bar_factory\" method=\"<caret>\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "create"
        );
    }

    public void testThatClassFactoryMethodAttributeProvidesCompletion() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <factory class=\"Foo\\Bar\" method=\"<caret>\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "create"
        );
    }
}
