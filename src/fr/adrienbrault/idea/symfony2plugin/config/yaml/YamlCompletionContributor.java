package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.completion.ConfigCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.PhpEntityClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelFieldLookupElement;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleFileCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.EventCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

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
            CompletionType.BASIC, YamlElementPatternHelper.getConfigKeyPattern(),
            new ConfigCompletionProvider()
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

                    for(ContainerParameter containerParameter: ContainerCollectionResolver.getParameters(parameters.getPosition().getProject()).values()) {
                        resultSet.addElement(new ParameterLookupElement(containerParameter, ParameterPercentWrapInsertHandler.getInstance(), element.getText()));
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
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("repositoryClass"), new RepositoryClassCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("mappedBy", "inversedBy"), new OrmRelationCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("_controller"), new ControllerCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("resource"), new SymfonyBundleFileCompletionProvider("Resources/config"));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSuperParentArrayKey("services"), new YamlCompletionProvider(new String[] {"class", "public", "tags", "calls", "arguments", "scope"}));

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("event")
        ), new EventCompletionProvider());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("alias")
        ), new FormAliasCompletionProvider());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("calls")
        ), new ServiceCallsMethodCompletion());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ), new ServiceCallsMethodTestCompletion());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("name")
        ), new TagNameCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("factory_method"), new ServiceClassMethodInsideScalarKeyCompletion("factory_service"));

    }


    private class ServiceCallsMethodTestCompletion extends CompletionProvider<CompletionParameters> {

        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();
            YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            yamlCompoundValue = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            addYamlClassMethods(yamlCompoundValue, completionResultSet, "class");

        }

    }

    private class ServiceClassMethodInsideScalarKeyCompletion extends CompletionProvider<CompletionParameters> {

        private String yamlArrayKeyName;

        public ServiceClassMethodInsideScalarKeyCompletion(String yamlArrayKeyName) {
            this.yamlArrayKeyName = yamlArrayKeyName;
        }

        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();
            YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            addYamlClassMethods(yamlCompoundValue, completionResultSet, this.yamlArrayKeyName);

        }

    }

    private class ServiceCallsMethodCompletion extends CompletionProvider<CompletionParameters> {

        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            // TODO: move this to pattern; filters match on parameter array
            // - [ setContainer, [ @service_container ] ]
            PsiElement psiElement = completionParameters.getPosition();
            if(psiElement.getParent() == null || !(psiElement.getParent().getContext() instanceof YAMLSequence)) {
                return;
            }

            YAMLKeyValue callYamlKeyValue = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
            if(callYamlKeyValue == null) {
                return;
            }

            addYamlClassMethods(callYamlKeyValue.getContext(), completionResultSet, "class");

        }

    }

    private static void addYamlClassMethods(@Nullable PsiElement psiElement, CompletionResultSet completionResultSet, String classTag) {

        if(psiElement == null) {
            return;
        }

        YAMLKeyValue classKeyValue = PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(classTag));
        if(classKeyValue == null) {
            return;
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText());
        if(phpClass != null) {
            PhpElementsUtil.addClassPublicMethodCompletion(completionResultSet, phpClass);
        }
    }

    private static class FormAliasCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();
            YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            yamlCompoundValue = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            String value = YamlHelper.getYamlKeyValueAsString(yamlCompoundValue, "class", true);
            if(value != null) {
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), value);
                if(phpClass != null) {
                    FormUtil.attachFormAliasesCompletions(phpClass, completionResultSet);
                }
            }

        }
    }

    private static class RepositoryClassCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                return;
            }

            PhpIndex phpIndex = PhpIndex.getInstance(parameters.getOriginalFile().getProject());
            for(PhpClass phpClass: phpIndex.getAllSubclasses("\\Doctrine\\ORM\\EntityRepository")) {
                String presentableFQN = phpClass.getPresentableFQN();
                if(presentableFQN != null) {
                    completionResultSet.addElement(LookupElementBuilder.create(phpClass.getName()).withTypeText(phpClass.getPresentableFQN(), true).withIcon(phpClass.getIcon()));
                }
            }


        }
    }

    private static class OrmRelationCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement position = completionParameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(position, YAMLCompoundValue.class);
            if(yamlCompoundValue == null) {
                return;
            }

            String className = YamlHelper.getYamlKeyValueAsString(yamlCompoundValue, "targetEntity", false);
            if(className == null) {
                return;
            }

            PhpClass phpClass = PhpElementsUtil.getClass(position.getProject(), className);
            if(phpClass == null) {
                return;
            }

            for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                if(field.getRelation() != null) {
                    completionResultSet.addElement(new DoctrineModelFieldLookupElement(field));
                }
            }

        }

    }

}

