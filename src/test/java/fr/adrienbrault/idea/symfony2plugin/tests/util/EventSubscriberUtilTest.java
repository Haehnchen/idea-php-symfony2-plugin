package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.EventSubscriberUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventSubscriberUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("EventSubscriber.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    /**
     * @see EventSubscriberUtil#getTaggedEventMethodParameter
     */
    public void testGetTaggedEventMethodParameterWithInternals() {
        assertEquals("\\Symfony\\Component\\HttpKernel\\Event\\GetResponseEvent", EventSubscriberUtil.getTaggedEventMethodParameter(getProject(), "kernel.request"));
    }

    /**
     * @see EventSubscriberUtil#getTaggedEventMethodParameter
     */
    public void testGetTaggedEventMethodParameterWithSubscriberInterface() {
        assertEquals("\\DateTime", EventSubscriberUtil.getTaggedEventMethodParameter(getProject(), "pre.foo"));
        assertNull(EventSubscriberUtil.getTaggedEventMethodParameter(getProject(), "pre.foo_doh"));
    }
    /**
     * @see EventSubscriberUtil#getTaggedEventMethodParameter
     */
    public void testGetTaggedEventMethodParameterWithIndexEventAnnotation() {
        assertEquals("Foo\\Event\\MyEvent", EventSubscriberUtil.getTaggedEventMethodParameter(getProject(), "my.foo.event"));
    }
}
