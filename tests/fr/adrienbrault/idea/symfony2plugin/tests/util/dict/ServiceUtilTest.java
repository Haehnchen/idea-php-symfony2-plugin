package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.io.File;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures/tags").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil#getPhpClassTags
     */
    public void testGetPhpClassTags() {
        Set<String> myTaggedClass = ServiceUtil.getPhpClassTags(PhpElementsUtil.getClass(getProject(), "MyTaggedClass"));
        assertContainsElements(myTaggedClass, "foo_datetime");
        assertContainsElements(myTaggedClass, "foo_iterator");
        assertDoesntContain(myTaggedClass, "foo_extends");
    }
}
