package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.ServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlCallServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.YamlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static ServiceSuggestionCollector[] COLLECTORS = new ServiceSuggestionCollector[] {
        new XmlConstructServiceSuggestionCollector(),
        new YamlConstructServiceSuggestionCollector(),
        new XmlCallServiceSuggestionCollector(),
    };

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        PsiElement element = parameters.getPosition();

        PrioritizedLookupResult result = getLookupElements(
            element, ContainerCollectionResolver.getServices(element.getProject()).values()
        );

        addPrioritizedServiceLookupElements(parameters, resultSet, result);
    }

    public static void addPrioritizedServiceLookupElements(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet, @NotNull PrioritizedLookupResult result) {
        // move known elements to top
        if(result.getTopStrings().size() > 0) {
            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(parameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new MyLookupElementWeigher(result.getTopStrings()));

            resultSet = resultSet.withRelevanceSorter(completionSorter);
        }

        resultSet.addAllElements(result.getLookupElements());
    }

    @NotNull
    public static PrioritizedLookupResult getLookupElements(@Nullable PsiElement element, @NotNull Collection<ContainerService> services) {
        // collect instance to highlight services
        List<String> servicesForInstance = new ArrayList<>();

        if(element != null) {
            Set<String> servicesForInstanceSet = new java.util.HashSet<>();

            for (ServiceSuggestionCollector collector : COLLECTORS) {
                servicesForInstanceSet.addAll(collector.collect(element, services));
            }

            servicesForInstance.addAll(
                ServiceContainerUtil.getSortedServiceId(element.getProject(), servicesForInstanceSet)
            );
        }

        Collection<LookupElement> collect = services.stream()
            .map((Function<ContainerService, LookupElement>)
                service -> new ServiceStringLookupElement(service, servicesForInstance.contains(service.getName())))
            .collect(Collectors.toList());

        return new PrioritizedLookupResult(collect, servicesForInstance);
    }

    public static class PrioritizedLookupResult {

        @NotNull
        private final Collection<LookupElement> lookupElements;

        @NotNull
        private final List<String> topStrings;

        public PrioritizedLookupResult(@NotNull Collection<LookupElement> lookupElements, @NotNull List<String> topStrings) {
            this.lookupElements = lookupElements;
            this.topStrings = topStrings;
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            return lookupElements;
        }

        @NotNull
        public List<String> getTopStrings() {
            return topStrings;
        }
    }

    public static class MyLookupElementWeigher extends LookupElementWeigher {
        @NotNull
        private final List<String> elements;

        public MyLookupElementWeigher(@NotNull List<String> elements) {
            super("topElement");
            this.elements = elements;

            // top most element ist first
            Collections.reverse(elements);
        }

        @Nullable
        @Override
        public Comparable weigh(@NotNull LookupElement element) {
            String lookupString = element.getLookupString();
            if(!elements.contains(lookupString)) {
                return 0;
            }

            // start by "0"
            int pos = elements.indexOf(lookupString) + 1;

            // we reversed the list so, top most element has higher negative value now
            return -1000 - pos;
        }
    }
}
