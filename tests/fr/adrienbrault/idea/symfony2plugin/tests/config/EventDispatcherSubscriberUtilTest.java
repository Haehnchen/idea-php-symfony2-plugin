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

        EventDispatcherSubscribedEvent event3 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "pre.foo1").iterator().next();
        assertEquals("pre.foo1", event3.getStringValue());
        assertEquals("onStoreOrder", event3.getMethodName());

        EventDispatcherSubscribedEvent event4 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "pre.foo2").iterator().next();
        assertEquals("pre.foo2", event4.getStringValue());
        assertEquals("onKernelResponseMid", event4.getMethodName());

        EventDispatcherSubscribedEvent event5 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "pre.foo3").iterator().next();
        assertEquals("pre.foo3", event5.getStringValue());
        assertNull(event5.getMethodName());

        EventDispatcherSubscribedEvent event6 = EventDispatcherSubscriberUtil.getSubscribedEvent(getProject(), "pre.foo4").iterator().next();
        assertEquals("pre.foo4", event6.getStringValue());
        assertNull( event6.getMethodName());
    }

}
