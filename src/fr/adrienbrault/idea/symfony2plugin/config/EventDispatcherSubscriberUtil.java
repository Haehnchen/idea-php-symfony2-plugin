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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EventDispatcherSubscriberUtil {

    @NotNull
    public static List<EventDispatcherSubscribedEvent> getSubscribedEvents(@NotNull Project project) {

        List<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

        // http://symfony.com/doc/current/components/event_dispatcher/introduction.html
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface");

        for(PhpClass phpClass: phpClasses) {

            if(PhpElementsUtil.isTestClass(phpClass)) {
                continue;
            }

            Method method = phpClass.findMethodByName("getSubscribedEvents");
            if(method != null) {
                PhpReturn phpReturn = PsiTreeUtil.findChildOfType(method, PhpReturn.class);
                if(phpReturn != null) {
                    attachSubscriberEventNames(events, phpClass, phpReturn);
                }
            }
        }

       return events;
    }

    private static void attachSubscriberEventNames(@NotNull List<EventDispatcherSubscribedEvent> events, @NotNull PhpClass phpClass, @NotNull PhpReturn phpReturn) {

        PhpPsiElement array = phpReturn.getFirstPsiChild();
        if(!(array instanceof ArrayCreationExpression)) {
            return;
        }

        String presentableFQN = phpClass.getPresentableFQN();
        if(presentableFQN == null) {
            return;
        }

        Iterable<ArrayHashElement> arrayHashElements = ((ArrayCreationExpression) array).getHashElements();
        for(ArrayHashElement arrayHashElement: arrayHashElements) {
            PsiElement arrayKey = arrayHashElement.getKey();

            // support string and constants
            if(arrayKey instanceof StringLiteralExpression) {

                // ['doh' => 'method']
                events.add(new EventDispatcherSubscribedEvent(
                    ((StringLiteralExpression) arrayKey).getContents(),
                    presentableFQN,
                    PhpElementsUtil.getStringValue(arrayHashElement.getValue())
                ));

            } else if(arrayKey instanceof PhpReference) {
                String resolvedString = PhpElementsUtil.getStringValue(arrayKey);
                if(resolvedString != null) {

                    // [FOO::BAR => 'method']
                    events.add(new EventDispatcherSubscribedEvent(
                        resolvedString,
                        presentableFQN,
                        PhpElementsUtil.getStringValue(arrayHashElement.getValue()),
                        ((PhpReference) arrayKey).getSignature())
                    );
                }

            }

        }

    }

    @NotNull
    public static List<EventDispatcherSubscribedEvent> getSubscribedEvent(@NotNull Project project, @NotNull String eventName) {

        List<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

        for(EventDispatcherSubscribedEvent event: getSubscribedEvents(project)) {
            if(event.getStringValue().equals(eventName)) {
                events.add(event);
            }
        }

        return events;
    }

    @NotNull
    public static List<PsiElement> getEventPsiElements(@NotNull Project project, @NotNull String eventName) {

        List<PsiElement> psiElements = new ArrayList<PsiElement>();

        // @TODO: remove
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

