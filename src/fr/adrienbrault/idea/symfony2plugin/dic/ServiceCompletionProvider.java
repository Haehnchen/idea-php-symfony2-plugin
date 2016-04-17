package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ArrayListSet;
import com.intellij.util.containers.HashSet;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.ServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.YamlConstructServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ServiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static ServiceSuggestionCollector[] COLLECTORS = new ServiceSuggestionCollector[] {
        new XmlConstructServiceSuggestionCollector(),
        new YamlConstructServiceSuggestionCollector(),
    };

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        PsiElement element = parameters.getPosition();

        resultSet.addAllElements(getLookupElements(
            element, ContainerCollectionResolver.getServices(element.getProject()).values())
        );
    }

    @NotNull
    public static Collection<LookupElement> getLookupElements(@Nullable PsiElement element, @NotNull Collection<ContainerService> services) {

        // collect instance to highlight services
        Collection<String> servicesForInstance = new HashSet<String>();

        if(element != null) {
            for (ServiceSuggestionCollector collector : COLLECTORS) {
                servicesForInstance.addAll(collector.collect(element, services));
            }
        }

        Collection<LookupElement> lookupElements = new ArrayListSet<LookupElement>();

        for(ContainerService containerService: services) {
            ServiceStringLookupElement lookupElement = new ServiceStringLookupElement(containerService)
                .setBoldText(servicesForInstance.contains(containerService.getName()));

            lookupElements.add(lookupElement);
        }

        return lookupElements;
    }
}
