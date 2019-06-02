package fr.adrienbrault.idea.symfony2plugin.tests.util.controller;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/controller/fixtures";
    }

    public void testThatControllerActionsForBundlesAreGenerated() {
        Collection<ControllerAction> actions = new ControllerIndex(getProject()).getActions();

        assertNotNull(
            actions.stream().findFirst().filter(action -> "FooBundle:Foo:foo".equals(action.getShortcutName()))
        );

        assertNotNull(
            actions.stream().findFirst().filter(action -> "FooBundle:Apple\\Foo:foo".equals(action.getShortcutName()))
        );

        assertNotNull(
            actions.stream().findFirst().filter(action -> "FooBundle:Apple\\Bar\\Foo:foo".equals(action.getShortcutName()))
        );
    }

    public void testThatSlashAndBackSlashAreNormalized() {
        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple\\Bar\\Foo:foo").iterator().next().getName());
        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple/Bar/Foo:foo").iterator().next().getName());
        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple/Bar:Foo/foo").iterator().next().getName());
        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple/Bar:Foo/fooAction").iterator().next().getName());

        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple///Bar:Foo////foo").iterator().next().getName());
        assertEquals("fooAction", ControllerIndex.getControllerMethod(getProject(), "FooBundle:Apple\\\\\\\\Bar:Foo\\\\foo").iterator().next().getName());
    }
}
