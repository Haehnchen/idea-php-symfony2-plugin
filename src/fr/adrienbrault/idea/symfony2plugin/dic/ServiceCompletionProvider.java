package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        PsiElement element = parameters.getOriginalPosition();

        if(element == null) {
            return;
        }


        Set<String> serviceNameLookup = new HashSet<String>();

        Map<String,String> map = ServiceXmlParserFactory.getInstance(element.getProject(), XmlServiceParser.class).getServiceMap().getMap();
        for( Map.Entry<String, String> entry: map.entrySet() ) {
            serviceNameLookup.add(entry.getKey());
            resultSet.addElement(
                new ServiceStringLookupElement(entry.getKey(), entry.getValue())
            );
        }

        // local file services
        for( Map.Entry<String, String> entry: YamlHelper.getLocalServiceMap(element).entrySet()) {
            if(!serviceNameLookup.contains(entry.getKey())) {
                resultSet.addElement(
                    new ServiceStringLookupElement(entry.getKey(), entry.getValue())
                );
            }
        }

        // all service name that for indexed
        for(String serviceName: ServiceIndexUtil.getAllServiceNames(element.getProject())) {
            if(!serviceNameLookup.contains(serviceName)) {
                resultSet.addElement(
                    new ServiceStringLookupElement(serviceName)
                );
            }
        }

    }
}
