package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.DispatcherEvent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.EventAnnotationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.EventSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherSubscriberUtil {

    private static final Key<CachedValue<Collection<EventDispatcherSubscribedEvent>>> EVENT_SUBSCRIBERS = new Key<>("SYMFONY_EVENT_SUBSCRIBERS");
    private static final Key<CachedValue<Set<String>>> EVENT_ANNOTATIONS = new Key<>("SYMFONY_EVENT_ANNOTATIONS");

    @NotNull
    public static Collection<EventDispatcherSubscribedEvent> getSubscribedEvents(final @NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            EVENT_SUBSCRIBERS,
            () -> CachedValueProvider.Result.create(getSubscribedEventsProxy(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static Collection<EventDispatcherSubscribedEvent> getSubscribedEventsProxy(@NotNull Project project) {

        Collection<EventDispatcherSubscribedEvent> events = new ArrayList<>();

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

    private static void attachSubscriberEventNames(@NotNull Collection<EventDispatcherSubscribedEvent> events, @NotNull PhpClass phpClass, @NotNull PhpReturn phpReturn) {

        PhpPsiElement array = phpReturn.getFirstPsiChild();
        if(!(array instanceof ArrayCreationExpression)) {
            return;
        }

        String presentableFQN = phpClass.getPresentableFQN();

        Iterable<ArrayHashElement> arrayHashElements = ((ArrayCreationExpression) array).getHashElements();
        for(ArrayHashElement arrayHashElement: arrayHashElements) {
            PsiElement arrayKey = arrayHashElement.getKey();

            PsiElement value = null;
            // get method name
            // @TODO: support multiple method names, currently we only use method name if type hint, so first item helps for now
            Collection<PsiElement> subscriberMethods = getSubscriberMethods(arrayHashElement);
            if(!subscriberMethods.isEmpty()) {
                value = subscriberMethods.iterator().next();
            }

            if(arrayKey instanceof StringLiteralExpression) {

                // ['doh' => 'method']
                events.add(new EventDispatcherSubscribedEvent(
                    ((StringLiteralExpression) arrayKey).getContents(),
                    presentableFQN,
                    PhpElementsUtil.getStringValue(value)
                ));

            } else if(arrayKey instanceof PhpReference) {
                String resolvedString = PhpElementsUtil.getStringValue(arrayKey);
                if(resolvedString != null) {

                    // [FOO::BAR => 'method']
                    events.add(new EventDispatcherSubscribedEvent(
                        resolvedString,
                        presentableFQN,
                        PhpElementsUtil.getStringValue(value),
                        ((PhpReference) arrayKey).getSignature())
                    );
                }

            }

        }

    }

    /**
     * Extract method name for subscribe
     *
     * 'pre.foo1' => 'foo'
     * 'pre.foo1' => ['onStoreOrder', 0]
     * 'pre.foo2' => [['onStoreOrder', 0]]
     */
    @NotNull
    private static Collection<PsiElement> getSubscriberMethods(@NotNull ArrayHashElement arrayHashElement) {

        // support string, constants and array values
        PhpPsiElement value = arrayHashElement.getValue();
        if(value == null) {
            return Collections.emptySet();
        }

        // 'pre.foo' => [...]
        if(!(value instanceof ArrayCreationExpression)) {
            return new ArrayList<>(Collections.singletonList(value));
        }

        Collection<PsiElement> psiElements = new HashSet<>();

        // 'pre.foo' => [<caret>]
        PsiElement firstChild = value.getFirstPsiChild();
        if(firstChild != null && firstChild.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
            PhpPsiElement firstPsiChild = ((PhpPsiElement) firstChild).getFirstPsiChild();
            if(firstPsiChild instanceof StringLiteralExpression) {
                // 'pre.foo' => ['method']
                psiElements.add(firstPsiChild);
            } else if(firstPsiChild instanceof ArrayCreationExpression) {

                // 'pre.foo' => [['method', ...], ['method2', ...]]
                for (PsiElement psiElement : PsiElementUtils.getChildrenOfTypeAsList(firstPsiChild, PlatformPatterns.psiElement().withElementType(PhpElementTypes.ARRAY_VALUE))) {
                    if(!(psiElement instanceof PhpPsiElement)) {
                        continue;
                    }

                    PhpPsiElement prioValue = ((PhpPsiElement) psiElement).getFirstPsiChild();
                    if(prioValue instanceof StringLiteralExpression) {
                        psiElements.add(prioValue);
                    }
                }
            }
        }

        return psiElements;
    }

    @NotNull
    public static Collection<EventDispatcherSubscribedEvent> getSubscribedEvent(@NotNull Project project, @NotNull String eventName) {

        List<EventDispatcherSubscribedEvent> events = new ArrayList<>();

        for(EventDispatcherSubscribedEvent event: getSubscribedEvents(project)) {
            if(event.getStringValue().equals(eventName)) {
                events.add(event);
            }
        }

        return events;
    }

    @NotNull
    public static Collection<PsiElement> getEventPsiElements(@NotNull final Project project, final @NotNull String eventName) {

        final Collection<PsiElement> psiElements = new HashSet<>();

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

        final ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver.ServiceCollector.create(project);

        EventSubscriberUtil.visitNamedTag(project, "kernel.event_listener", args -> {
            String event = args.getAttribute("event");
            if (StringUtils.isNotBlank(event) && eventName.equals(event)) {
                String serviceId = args.getServiceId();
                if(StringUtils.isNotBlank(serviceId)) {
                    String resolve = collector.resolve(serviceId);
                    if(resolve != null) {
                        psiElements.addAll(PhpElementsUtil.getClassesInterface(project, resolve));
                    }
                }
            }
        });

        // loading targets on @Event
        Set<String> annotationEvents = new HashSet<>();
        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(EventAnnotationStubIndex.KEY, eventName, GlobalSearchScope.allScope(project))) {
            FileBasedIndex.getInstance().processValues(EventAnnotationStubIndex.KEY, eventName, virtualFile, (virtualFile1, event) -> {
                if(event.getInstance() != null && StringUtils.isNotBlank(event.getInstance())) {
                    annotationEvents.add(event.getInstance());
                }
                return true;
            }, GlobalSearchScope.allScope(project));
        }

        // Convert class name from @Event; we need to do after collecting because of cross index access
        for (String instance : annotationEvents) {
            psiElements.addAll(PhpElementsUtil.getClassesInterface(project, instance));
        }

        return psiElements;
    }

    @NotNull
    public static Collection<LookupElement> getEventNameLookupElements(@NotNull Project project) {

        Map<String, LookupElement> results = new HashMap<>();

        XmlEventParser xmlEventParser = ServiceXmlParserFactory.getInstance(project, XmlEventParser.class);
        for(EventDispatcherSubscribedEvent event : xmlEventParser.getEvents()) {
            results.put(event.getStringValue(), LookupElementBuilder.create(event.getStringValue()).withTypeText(event.getType(), true).withIcon(Symfony2Icons.EVENT));
        }

        for(EventDispatcherSubscribedEvent event: EventDispatcherSubscriberUtil.getSubscribedEvents(project)) {
            results.put(event.getStringValue(), LookupElementBuilder.create(event.getStringValue()).withTypeText(event.getType(), true).withIcon(Symfony2Icons.EVENT));
        }

        EventSubscriberUtil.visitNamedTag(project, "kernel.event_listener", args -> {
            String event = args.getAttribute("event");
            if (event != null && StringUtils.isNotBlank(event)) {
                results.put(event, LookupElementBuilder.create(event).withTypeText("kernel.event_listener", true).withIcon(Symfony2Icons.EVENT));
            }
        });

        for (String s : FileIndexCaches.getIndexKeysCache(project, EVENT_ANNOTATIONS, EventAnnotationStubIndex.KEY)) {

            String typeText = "Event";

            // Find class name on fast index
            DispatcherEvent event = ContainerUtil.find(FileBasedIndex.getInstance().getValues(
                EventAnnotationStubIndex.KEY, s, GlobalSearchScope.allScope(project)),
                dispatcherEvent -> dispatcherEvent.getInstance() != null
            );

            if(event != null) {
                typeText = event.getInstance();
            }

            results.put(s, LookupElementBuilder.create(s).withTypeText(typeText, true).withIcon(Symfony2Icons.EVENT));
        }

        return results.values();
    }


    /**
     * XML: <tag event=""fooBar/>
     * YML: - event: 'foobar'
     * PHP: ['foo' => 'method']
     */
    @Nullable
    public static String getEventNameFromScope(@NotNull PsiElement psiElement) {
        // xml service
        if(psiElement.getContainingFile() instanceof XmlFile) {
            XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(xmlTag == null) {
                return null;
            }

            XmlAttribute event = xmlTag.getAttribute("event");
            if(event == null) {
                return null;
            }

            String value = event.getValue();
            if(StringUtils.isBlank(value)) {
                return null;
            }

            return value;
        } else if(psiElement.getContainingFile() instanceof YAMLFile) {
            // yaml services
            YAMLMapping yamlHash = PsiTreeUtil.getParentOfType(psiElement, YAMLMapping.class);
            if(yamlHash != null) {
                YAMLKeyValue event = YamlHelper.getYamlKeyValue(yamlHash, "event");
                if(event != null) {
                    PsiElement value = event.getValue();
                    if(value != null ) {
                        String text = value.getText();
                        if(StringUtils.isNotBlank(text)) {
                            return text;
                        }
                    }
                }
            }
        } else if(psiElement.getContainingFile() instanceof PhpFile) {
            ArrayHashElement arrayHashElement = PsiTreeUtil.getParentOfType(psiElement, ArrayHashElement.class);
            if(arrayHashElement != null) {
                PhpPsiElement key = arrayHashElement.getKey();
                if (key != null) {
                    String stringValue = PhpElementsUtil.getStringValue(key);
                    if(StringUtils.isNotBlank(stringValue)) {
                        return stringValue;
                    }
                }
            }
        }

        return null;
    }
}

