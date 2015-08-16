package fr.adrienbrault.idea.symfony2plugin.tests.util.dict;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.io.File;
import java.util.Map;
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
     * @see ServiceUtil#getTaggedInstances
     */
    public void testInstance() {
        Map<String, Set<String>> myTaggedClass = ServiceUtil.getTaggedInstances(getProject(), PhpElementsUtil.getClass(getProject(), "MyTaggedClass"));
        assertTrue(myTaggedClass.get("foo_datetime").contains("\\DateTime"));
        assertTrue(myTaggedClass.get("foo_iterator").contains("\\Iterator"));
        assertFalse(myTaggedClass.containsKey("foo_iterator_aggregate"));
    }
}
