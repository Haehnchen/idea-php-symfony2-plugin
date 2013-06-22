package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.PhpEntityClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleFileCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.EventCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {
    public YamlCompletionContributor() {

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getServiceDefinition(),
            new ServiceCompletionProvider()
        );

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getServiceParameterDefinition(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement element = parameters.getOriginalPosition();

                    if(element == null) {
                        return;
                    }

                    Map<String, String> it = ServiceXmlParserFactory.getInstance(element.getProject(), ParameterServiceParser.class).getParameterMap();
                    for(Map.Entry<String, String> Entry: it.entrySet()) {
                        resultSet.addElement(new ParameterLookupElement(Entry.getKey(), Entry.getValue(), ParameterPercentWrapInsertHandler.getInstance(), element));
                    }

                }
            }
        );


        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("type"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getTypes()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("nullable"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getNullAble()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmParentLookup("joinColumn"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getJoinColumns()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmRoot(), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getRootItems()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("fields", "id"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getPropertyMappings()));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("oneToOne"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToOne)));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("oneToMany"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToMany)));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("manyToOne"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToOne)));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("manyToMany"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToMany)));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class"), new PhpClassAndParameterCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("factory_service", "parent"), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getParameterClassPattern(), new PhpClassCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("targetEntity"), new PhpEntityClassCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("_controller"), new ControllerCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("resource"), new SymfonyBundleFileCompletionProvider("Resources/config"));

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("event")
        ), new EventCompletionProvider());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("name")
        ), new TagNameCompletionProvider());

    }

}

