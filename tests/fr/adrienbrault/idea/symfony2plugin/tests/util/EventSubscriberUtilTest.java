package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.EventSubscriberUtil;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventSubscriberUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("EventSubscriber.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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

}
