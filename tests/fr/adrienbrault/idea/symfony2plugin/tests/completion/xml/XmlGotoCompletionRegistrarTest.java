package fr.adrienbrault.idea.symfony2plugin.tests.completion.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.completion.xml.XmlGotoCompletionRegistrar
 */
public class XmlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureByText("config_foo.xml", "<foo/>");
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
}
