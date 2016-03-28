package fr.adrienbrault.idea.symfony2plugin.tests.navigation.controller;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.navigation.controller.RouteControllerRelatedGotoCollector
 */
public class RouteControllerRelatedGotoCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("routing.yml"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatControllerProvidesYamDefinitionNavigation() {
        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo\\Route {\n" +
            "   class Bar {\n" +
            "       function fooAction() {}\n" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("foo_route_bar_foo"));

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Foo\\Route {\n" +
            "   class Bar {\n" +
            "       function barAction() {}\n" +
            "   }\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("foo_route_bar_bar_sequence"));
    }

}
