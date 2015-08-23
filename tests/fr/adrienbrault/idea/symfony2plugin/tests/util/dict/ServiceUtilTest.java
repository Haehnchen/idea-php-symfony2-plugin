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
     * @see ServiceUtil#getPhpClassTags
     */
    public void testInstance() {
        Set<String> myTaggedClass = ServiceUtil.getPhpClassTags(PhpElementsUtil.getClass(getProject(), "MyTaggedClass"));
        assertTrue(myTaggedClass.contains("foo_extends"));
        assertTrue(myTaggedClass.contains("foo_iterator_aggregate"));
        assertFalse(myTaggedClass.contains("foo_extends"));
    }
}
