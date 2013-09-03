package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class EventDispatcherSubscriberUtil {

    public static ArrayList<EventDispatcherSubscribedEvent> getSubscribedEvents(Project project) {

        ArrayList<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

        // http://symfony.com/doc/current/components/event_dispatcher/introduction.html
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface");

        for(PhpClass phpClass: phpClasses) {
            PsiElement method = PhpElementsUtil.getPsiElementsBySignatureSingle(project, "#M#C\\" + phpClass.getPresentableFQN() + ".getSubscribedEvents");
            if(method instanceof Method) {
                PhpReturn phpReturn = PsiTreeUtil.findChildOfType(method, PhpReturn.class);
                if(phpReturn != null) {
                    PhpPsiElement array = phpReturn.getFirstPsiChild();
                    if(array instanceof ArrayCreationExpression) {
                        Iterable<ArrayHashElement> arrayHashElements = ((ArrayCreationExpression) array).getHashElements();
                        for(ArrayHashElement arrayHashElement: arrayHashElements) {
                            PsiElement arrayKey = arrayHashElement.getKey();

                            // support string and constants
                            if(arrayKey instanceof StringLiteralExpression) {
                                events.add(new EventDispatcherSubscribedEvent(((StringLiteralExpression) arrayKey).getContents(), phpClass.getPresentableFQN()));
                            } else if(arrayKey instanceof PhpReference) {
                                PsiReference psiReference = arrayKey.getReference();
                                if(psiReference != null) {
                                    PsiElement ref = psiReference.resolve();
                                    if(ref instanceof Field) {
                                        PsiElement resolved = ((Field) ref).getDefaultValue();
                                        if(resolved instanceof StringLiteralExpression)
                                            events.add(new EventDispatcherSubscribedEvent(((StringLiteralExpression) resolved).getContents(), phpClass.getPresentableFQN(), ((PhpReference) arrayKey).getSignature()));
                                        }
                                    }
                                }

                            }

                        }

                    }

                }

            }


            return events;
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

}

