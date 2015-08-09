package fr.adrienbrault.idea.symfony2plugin.tests.config;

import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherSubscriberUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("EventSubscriber.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see EventDispatcherSubscriberUtil#getSubscribedEvents
     * @see EventDispatcherSubscriberUtil#attachSubscriberEventNames
     */
    public void testGetSubscribedEvent() {
        EventDispatcherSubscribedEvent event1 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "pre.foo").iterator().next();
        assertEquals("pre.foo", event1.getStringValue());
        assertEquals("TestEventSubscriber", event1.getFqnClassName());
        assertNull(event1.getSignature());
        assertEquals("preFoo", event1.getMethodName());

        EventDispatcherSubscribedEvent event2 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "post.foo").iterator().next();
        assertEquals("post.foo", event2.getStringValue());
        assertEquals("TestEventSubscriber", event2.getFqnClassName());
        assertEquals("#K#C\\Foo\\Bar.BAR", event2.getSignature());
        assertEquals("postFoo", event2.getMethodName());
    }

}
