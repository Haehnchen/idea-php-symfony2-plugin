package fr.adrienbrault.idea.symfonyplugin.tests.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfonyplugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherSubscriberUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("EventSubscriber.php");
        myFixture.copyFileToProject("EventSubscriberAnnotation.php");

        myFixture.copyFileToProject("event.services.xml");
        myFixture.copyFileToProject("event.services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/config/fixtures";
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

    /**
     * @see EventDispatcherSubscriberUtil#getEventNameLookupElements
     */
    public void testGetEventNameLookupElementsForEventAnnotations() {
        Collection<LookupElement> eventNameLookupElements = EventDispatcherSubscriberUtil.getEventNameLookupElements(getProject());
        ContainerUtil.find(eventNameLookupElements, lookupElement ->
            lookupElement.getLookupString().equals("bar.pre_bar")
        );

        ContainerUtil.find(eventNameLookupElements, lookupElement ->
            lookupElement.getLookupString().equals("bar.post_bar")
        );

        ContainerUtil.find(eventNameLookupElements, lookupElement -> {
            if(!"bar.post_bar".equals(lookupElement.getLookupString())) {
                return false;
            }

            LookupElementPresentation lookupElementPresentation = new LookupElementPresentation();
            lookupElement.renderElement(lookupElementPresentation);

            return "My\\MyFooEvent".equals(lookupElementPresentation.getTypeText());
        });
    }

    /**
     * @see EventDispatcherSubscriberUtil#getEventPsiElements
     */
    public void testGetEventTargetsElementsForTags() {
        Collection<PsiElement> elements = EventDispatcherSubscriberUtil.getEventPsiElements(getProject(), "kernel.exception.xml");

        assertNotNull(
            ContainerUtil.find(elements, psiElement -> psiElement instanceof PhpClass && ((PhpClass) psiElement).getFQN().contains("MyDateTime"))
        );
    }

    /**
     * @see EventDispatcherSubscriberUtil#getEventPsiElements
     */
    public void testGetEventTargetsElementsForEventAnnotations() {
        Collection<PsiElement> elements = EventDispatcherSubscriberUtil.getEventPsiElements(getProject(), "bar.post_bar");

        assertNotNull(
            ContainerUtil.find(elements, psiElement -> psiElement instanceof PhpClass && ((PhpClass) psiElement).getFQN().contains("MyFooEvent"))
        );
    }

    /**
     * @see EventDispatcherSubscriberUtil#getEventNameLookupElements
     */
    public void testGetEventNameLookupElementsForTaggedKernelListener() {
        Collection<LookupElement> eventNameLookupElements = EventDispatcherSubscriberUtil.getEventNameLookupElements(getProject());

        ContainerUtil.find(eventNameLookupElements, lookupElement ->
            lookupElement.getLookupString().equals("kernel.exception.xml")
        );

        ContainerUtil.find(eventNameLookupElements, lookupElement ->
            lookupElement.getLookupString().equals("kernel.exception.yml")
        );
    }

    /**
     * @see EventDispatcherSubscriberUtil#getEventNameFromScope
     */
    public void testGetEventNameFromScope() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php ['foo' => 'foo<caret>bar']");
        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertEquals("foo", EventDispatcherSubscriberUtil.getEventNameFromScope(psiElement));
    }
}
