package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;

import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.CompletionContributor;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {
    public YamlCompletionContributor() {
        extend(
                CompletionType.BASIC, YamlElementPatternHelper.getServiceDefinition(),
                new CompletionProvider<CompletionParameters>() {
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {

                        PsiElement element = parameters.getOriginalPosition();

                        if(element == null) {
                            return;
                        }

                        Symfony2ProjectComponent symfony2ProjectComponent = element.getProject().getComponent(Symfony2ProjectComponent.class);
                        Map<String,String> map = symfony2ProjectComponent.getServicesMap().getMap();

                        for( Map.Entry<String, String> entry: map.entrySet() ) {
                            resultSet.addElement(
                                    new ServiceStringLookupElement(entry.getKey(), entry.getValue())
                            );
                        }

                    }
                }
        );


        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("type"), new YamlCompletionProvider(DoctrineStaticTypeLookupBuilder.getTypes()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("nullable"), new YamlCompletionProvider(DoctrineStaticTypeLookupBuilder.getNullAble()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmParentLookup("joinColumn"), new YamlCompletionProvider(DoctrineStaticTypeLookupBuilder.getJoinColumns()));


    }

}

