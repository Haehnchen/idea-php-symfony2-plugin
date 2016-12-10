package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.ServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlCallServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.YamlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        Collection<String> servicesForInstance = new HashSet<>();

        if(element != null) {
            for (ServiceSuggestionCollector collector : COLLECTORS) {
                servicesForInstance.addAll(collector.collect(element, services));
            }
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
        private final Collection<String> topStrings;

        public PrioritizedLookupResult(@NotNull Collection<LookupElement> lookupElements, @NotNull Collection<String> topStrings) {
            this.lookupElements = lookupElements;
            this.topStrings = topStrings;
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            return lookupElements;
        }

        @NotNull
        public Collection<String> getTopStrings() {
            return topStrings;
        }
    }

    private static class MyLookupElementWeigher extends LookupElementWeigher {
        @NotNull
        private final Collection<String> elements;

        MyLookupElementWeigher(@NotNull Collection<String> elements) {
            super("topElement");
            this.elements = elements;
        }

        @Nullable
        @Override
        public Comparable weigh(@NotNull LookupElement element) {
            String lookupString = element.getLookupString();
            if(!elements.contains(lookupString)) {
                return 0;
            }

            // we dont want eg ".debug." service first
            if(ContainerUtil.find(SymfonyCreateService.ContainerServicePriorityNameComparator.LOWER_PRIORITY, lookupString::contains) != null) {
                return -999;
            }

            return -1000;
        }
    }
}
