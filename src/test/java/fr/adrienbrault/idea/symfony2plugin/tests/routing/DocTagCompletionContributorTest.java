package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.util.DocHashTagReferenceContributor
 */
public class DocTagCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes_routing.php");
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("routing.yml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("routing.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpRouteReferenceContributor.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testRouteDocTagCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php new TestClass('<caret>');", "route_foo");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php new TestClass('<caret>');", "route_bar");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php new TestClass('<caret>');", "xml_route");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php new TestClass('<caret>');", "foo_bar");

        assertCompletionContains(PhpFileType.INSTANCE,  "<?php (new TestClass())->foo('<caret>');", "route_foo");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php (new TestClass())->foo('<caret>');", "route_bar");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php (new TestClass())->foo('<caret>');", "xml_route");
        assertCompletionContains(PhpFileType.INSTANCE,  "<?php (new TestClass())->foo('<caret>');", "foo_bar");
    }

}
