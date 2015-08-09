package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventSubscriberUtil {

    @Nullable
    public static String getTaggedEventMethodParameter(Project project, String eventName) {

        // strong internal events
        if(ServiceUtil.TAGS.containsKey(eventName)) {
            return ServiceUtil.TAGS.get(eventName);
        }

        EventsCollector[] collectors = new EventsCollector[] {
            new XmlEventsCollector(),
        };

        for (String service : ServiceUtil.getTaggedServices(project, "kernel.event_listener")) {

            for (PsiElement psiElement : ServiceIndexUtil.findServiceDefinitions(project, service)) {

                Collection<String> methods = new HashSet<String>();

                for (EventsCollector collector : collectors) {
                    methods.addAll(collector.collect(psiElement, eventName));
                }

                // find a method
                PhpClass phpClass = ServiceUtil.getServiceClass(project, service);
                if(phpClass != null) {
                    for (String methodName : methods) {
                        Method method = phpClass.findMethodByName(methodName);
                        if(method != null) {
                            String methodParameterClassHint = PhpElementsUtil.getMethodParameterTypeHint(method);
                            if(methodParameterClassHint != null) {
                                return methodParameterClassHint;
                            }
                        }
                    }
                }

            }

        }

        return null;
    }

    public interface EventsCollector {
        Collection<String> collect(@NotNull PsiElement psiElement, @NotNull String eventName);
    }

    /**
     * <tag name="kernel.event_listener" event="event_bar" method="foo" />
     */
    private static class XmlEventsCollector implements EventsCollector {

        public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull String eventName) {

            if(!(psiElement instanceof XmlTag)) {
                return Collections.emptySet();
            }

            Collection<String> methods = new HashSet<String>();
            for (XmlTag tag : ((XmlTag) psiElement).findSubTags("tag")) {

                if(!"kernel.event_listener".equals(tag.getAttributeValue("name")) || !eventName.equals(tag.getAttributeValue("event"))) {
                    continue;
                }

                String methodName = tag.getAttributeValue("method");
                if(StringUtils.isBlank(methodName)) {
                    continue;
                }

                methods.add(methodName);
            }

            return methods;
        }

    }

}
