package fr.adrienbrault.idea.symfony2plugin.tests.completion.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
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
        myFixture.copyFileToProject("routes.xml");
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

    public void testIdInsideServiceTagMustCompleteWithClassNameOnShortcut() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<services><service id=\"<caret>\"/></services>",
            "Foobar"
        );

        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<services><service id=\"Foo\\<caret>\"/></services>",
            "Foo\\Bar"
        );
    }

    public void testThatServiceAliasAttributeMustProvideCompletion() {
        assertCompletionContains(
            XmlFileType.INSTANCE,
            "<services><service alias=\"<caret>\"></services>",
            "foo.bar_factory"
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

    public void testThatRouteInsideRouteDefaultKeyCompletedAndNavigable() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "    <route id=\"root\" path=\"/wp-admin\">\n" +
                "        <default key=\"route\"><caret></default>\n" +
                "    </route>",
            "foo_route"
        );

        assertNavigationMatch(XmlFileType.INSTANCE, "" +
                "    <route id=\"root\" path=\"/wp-admin\">\n" +
                "        <default key=\"route\">foo_<caret>route</default>\n" +
                "    </route>"
        );
    }

    public void testThatDecoratesServiceTagProvidesReferences() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service class=\"Foo\\Foobar\" decorates=\"<caret>\"/>\n" +
                "    </services>\n" +
                "</container>\n",
            "service_container"
        );

        assertNavigationMatch(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service class=\"Foo\\Foobar\" decorates=\"foo.bar_<caret>factory\"/>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

    public void testThatDecoratesPrioritizeLookupElementOnInstance() {
        assertCompletionLookupContainsPresentableItem(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service class=\"Foo\\Foobar\" decorates=\"<caret>\"/>\n" +
                "    </services>\n" +
                "</container>\n",
            lookupElement -> "foo.bar_factory".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold() && lookupElement.isItemTextBold()
        );
    }

    public void testNamedArgumentKeyCompletionWithMethodParameterVariables() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Bar\">\n" +
                "           <argument key=\"<caret>\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "$foo", "$foo3"
        );

        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Bar\">\n" +
                "           <call method=\"setBar\">\n" +
                "               <argument key=\"<caret>\"/>\n" +
                "           </call>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "$bar", "$bar3"
        );
    }

    public void testNamedArgumentKeyNavigationWithMethodParameterVariables() {
        assertNavigationMatch("services.xml", "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Bar\">\n" +
                "           <argument key=\"$f<caret>oo\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("services.xml", "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Bar\">\n" +
                "           <call method=\"setBar\">\n" +
                "               <argument key=\"$b<caret>ar\"/>\n" +
                "           </call>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement()
        );
    }
}
