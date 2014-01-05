package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.ArrayList;
import java.util.Collection;

public class EventDispatcherSubscriberUtil {

    public static ArrayList<EventDispatcherSubscribedEvent> getSubscribedEvents(Project project) {

        ArrayList<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

        // http://symfony.com/doc/current/components/event_dispatcher/introduction.html
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface");

        for(PhpClass phpClass: phpClasses) {
            if(!PhpUnitUtil.isTestClass(phpClass)) {
                Method method = PhpElementsUtil.getClassMethod(phpClass, "getSubscribedEvents");

                if(method != null) {
                    PhpReturn phpReturn = PsiTreeUtil.findChildOfType(method, PhpReturn.class);
                    if(phpReturn != null) {
                        attachSubscriberEventNames(events, phpClass, phpReturn);
                    }

                }
            }
        }

       return events;
    }

    private static void attachSubscriberEventNames(ArrayList<EventDispatcherSubscribedEvent> events, PhpClass phpClass, PhpReturn phpReturn) {

        PhpPsiElement array = phpReturn.getFirstPsiChild();
        if(!(array instanceof ArrayCreationExpression)) {
            return;
        }

        Iterable<ArrayHashElement> arrayHashElements = ((ArrayCreationExpression) array).getHashElements();
        for(ArrayHashElement arrayHashElement: arrayHashElements) {
            PsiElement arrayKey = arrayHashElement.getKey();

            // support string and constants
            if(arrayKey instanceof StringLiteralExpression) {
                events.add(new EventDispatcherSubscribedEvent(((StringLiteralExpression) arrayKey).getContents(), phpClass.getPresentableFQN()));
            } else if(arrayKey instanceof PhpReference) {
                String resolvedString = PhpElementsUtil.getStringValue(arrayKey);
                if(resolvedString != null) {
                    events.add(new EventDispatcherSubscribedEvent(resolvedString, phpClass.getPresentableFQN(), ((PhpReference) arrayKey).getSignature()));
                }

            }

        }

    }

    public static ArrayList<EventDispatcherSubscribedEvent> getSubscribedEvent(Project project, String eventName) {

        ArrayList<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

        for(EventDispatcherSubscribedEvent event: getSubscribedEvents(project)) {
            if(event.getStringValue().equals(eventName)) {
                events.add(event);
            }
        }

        return events;
    }

    public static ArrayList<PsiElement> getEventPsiElements(Project project, String eventName) {

        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();

        XmlEventParser xmlEventParser = ServiceXmlParserFactory.getInstance(project, XmlEventParser.class);
        for(EventDispatcherSubscribedEvent event : xmlEventParser.getEventSubscribers(eventName)) {
            PhpClass phpClass = PhpElementsUtil.getClass(project, event.getFqnClassName());
            if(phpClass != null) {
                psiElements.add(phpClass);
            }
        }

        for(EventDispatcherSubscribedEvent event: EventDispatcherSubscriberUtil.getSubscribedEvent(project, eventName)) {
            PhpClass phpClass = PhpElementsUtil.getClass(project, event.getFqnClassName());
            if(phpClass != null) {
                psiElements.add(phpClass);
            }
        }

        return psiElements;
    }

}

