package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;

import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

import com.intellij.codeInsight.completion.CompletionContributor;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {
    public YamlCompletionContributor() {
        extend(
                CompletionType.BASIC,
                // @TODO: look if we can filter more here
                PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withText(StandardPatterns.string().startsWith("@")).withLanguage(YAMLLanguage.INSTANCE),
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
    }

}

