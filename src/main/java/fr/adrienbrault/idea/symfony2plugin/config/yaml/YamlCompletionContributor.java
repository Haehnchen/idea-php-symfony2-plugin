package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.completion.ConfigCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineYamlAnnotationLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.PhpEntityClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelFieldLookupElement;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleFileCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.EventCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {

    // @TODO: use xsd file
    // Symfony/Component/DependencyInjection/Loader/schema/dic/services/services-1.0.xsd
    private static final Map<String, String> SERVICE_KEYS = Collections.unmodifiableMap(new HashMap<>() {{
        put("class", "(string)");
        put("public", "(bool)");
        put("tags", null);
        put("alias", null);
        put("calls", null);
        put("arguments", null);
        put("synchronized", null);
        put("synthetic", null);
        put("lazy", "(bool)");
        put("abstract", null);
        put("parent", null);
        put("scope", "(request, prototype) <= 3.0");
        put("factory", ">= 2.6");
        put("factory_class", "<= 2.5");
        put("factory_service", "<= 2.5");
        put("factory_method", "<= 2.5");
        put("autowire", "(bool) >= 2.8");
        put("autowiring_types", ">= 2.8, < 4.0");
        put("deprecated", "(string) >= 2.8");
        put("decorates", null);
        put("decoration_inner_name", null);
        put("decoration_priority", "(int) >= 2.8");
        put("decoration_on_invalid", "(exception|ignore|null) >= 4.4");
        put("shared", "(bool) >= 3.0");
        put("resource", "(string) >= 3.3");
        put("autoconfigure", "(bool) >= 3.3");
        put("properties", ">= 5.1");
        put("configurator", ">= 2.8");
        put("bind", ">= 2.8");
        put("file", ">= 2.8");
        put("exclude", ">= 2.8");
    }});

    private static final Map<String, String> ROUTE_KEYS = Collections.unmodifiableMap(new HashMap<>() {{
        put("pattern", "deprecated");
        put("defaults", "(bool)");
        put("path", "(string)");
        put("requirements", "(array)");
        put("methods", "(array|string)");
        put("condition", "(string / expression)");
        put("resource", "(string)");
        put("prefix", "(string)");
        put("schemes", "(array|string)");
        put("host", "(string)");
        put("controller", "(string)");
    }});

    public YamlCompletionContributor() {

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getServiceDefinition(),
            new ServiceCompletionProvider()
        );

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getConfigKeyPattern(),
            new ConfigCompletionProvider()
        );

        // factory: ['@app.newsletter_manager_factory', createNewsletterManager]
        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getAfterCommaPattern(),
            new FactoryMethodCompletionProvider()
        );

        // factory: ['app.newsletter_manager_factory:createNewsletterManager']
        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("factory"),
            new MyFactoryStringMethodCompletionProvider()
        );

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getServiceParameterDefinition(),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement element = parameters.getOriginalPosition();

                    if (element == null) {
                        return;
                    }

                    for (ContainerParameter containerParameter : ContainerCollectionResolver.getParameters(parameters.getPosition().getProject()).values()) {
                        resultSet.addElement(new ParameterLookupElement(containerParameter, ParameterPercentWrapInsertHandler.getInstance(), element.getText()));
                    }

                    for (String s : DotEnvUtil.getEnvironmentVariables(element.getProject())) {
                        resultSet.addElement(new ParameterLookupElement(new ContainerParameter("env(" + s + ")", false), ParameterPercentWrapInsertHandler.getInstance(), element.getText()));
                        resultSet.addElement(new ParameterLookupElement(new ContainerParameter("env(resolve:" + s + ")", false), ParameterPercentWrapInsertHandler.getInstance(), element.getText()));
                    }
                }
            }
        );

        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("unique"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getNullAble()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("nullable"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getNullAble()));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("associationKey"), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getNullAble()));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmParentLookup("joinColumn"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\JoinColumn"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("joinColumns"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\JoinColumn"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmParentLookup("joinTable"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\JoinTable"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmRoot(), new YamlCompletionProvider(new DoctrineStaticTypeLookupBuilder().getRootItems()));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("id"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\Column", "associationKey"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("fields"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\Column"));

        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("oneToOne"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\OneToOne"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("oneToMany"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\OneToMany"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("manyToOne"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\ManyToOne"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getFilterOnPrevParent("manyToMany"), new DoctrineYamlAnnotationLookupBuilder("\\Doctrine\\ORM\\Mapping\\ManyToMany"));

        extend(CompletionType.BASIC,
            PlatformPatterns.and(
                YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class", "autowiring_types"),
                YamlElementPatternHelper.getThreeLevelKeyPattern("services")
            ),
            new PhpClassAndParameterCompletionProvider()
        );

        extend(CompletionType.BASIC, PlatformPatterns.and(
            YamlElementPatternHelper.getSingleLineScalarKey("factory_service", "parent"),
            YamlElementPatternHelper.getThreeLevelKeyPattern("services")
        ), new ServiceCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getParameterClassPattern(), new PhpClassCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("targetEntity", "targetDocument"), new PhpEntityClassCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("mappedBy", "inversedBy"), new OrmRelationCompletionProvider());
        extend(CompletionType.BASIC, YamlElementPatternHelper.getOrmSingleLineScalarKey("referencedColumnName"), new ReferencedColumnCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller"), new ControllerCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("resource"), new SymfonyBundleFileCompletionProvider("Resources/config", "Controller"));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("resource"), new DirectoryScopeCompletionProvider());

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSuperParentArrayKey("services"), new YamlCompletionProvider(SERVICE_KEYS));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getWithFirstRootKey(), new RouteKeyNameYamlCompletionProvider(ROUTE_KEYS));
        extend(CompletionType.BASIC, YamlElementPatternHelper.getParentKeyName("requirements"), new RouteRequirementsCompletion());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("event")
        ), new EventCompletionProvider());

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("alias")
        ), new FormAliasCompletionProvider());

        // - [ setContainer, [ @service_container ] ]
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getInsideKeyValue("calls"),
            new ServiceCallsMethodCompletion()
        );

        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ), new ServiceCallsMethodTestCompletion());

        // tags: { name: 'foobar' }
        extend(CompletionType.BASIC, StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("name")
        ), new TagNameCompletionProvider());

        // tags: [ foobar ]
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSequenceValueWithArrayKeyPattern("tags"),
            new TagNameCompletionProvider()
        );

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("factory_method"), new ServiceClassMethodInsideScalarKeyCompletion("factory_service"));

        // services:
        //   Foobar<caret>: ~
        //   Foobar\\<caret>: ~
        //   <caret>: ~
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getParentKeyName("services"),
            new MyServiceKeyAsClassCompletionParametersCompletionProvider()
        );

        // services:
        //  _defaults:
        //    bind:
        //      $<caret>: ''
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getNamedDefaultBindPattern(),
            new NamedArgumentCompletionProvider()
        );
    }

    /**
     * factory: "foo:<caret>"
     */
    private static class MyFactoryStringMethodCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            PsiElement parent = position.getParent();
            if(!(parent instanceof YAMLScalar)) {
                return;
            }

            String textValue = ((YAMLScalar) parent).getTextValue();
            String[] split = textValue.split(":");
            if(split.length < 2) {
                new ServiceCompletionProvider().addCompletions(parameters, processingContext, completionResultSet);
                return;
            }

            PhpClass phpClass = ServiceUtil.getServiceClass(position.getProject(), split[0]);
            if(phpClass == null) {
                return;
            }

            for (Method method : phpClass.getMethods()) {
                if(method.getAccess().isPublic() && !(method.getName().startsWith("__"))) {
                    completionResultSet.addElement(
                        LookupElementBuilder.create(split[0] + ":" + method.getName())
                        .withIcon(method.getIcon()).withTypeText(phpClass.getName(), true)
                    );
                }
            }
        }
    }

    private static class FactoryMethodCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            String service = YamlHelper.getPreviousSequenceItemAsText(position);
            if (service == null) {
                return;
            }

            PhpClass phpClass = ServiceUtil.getServiceClass(position.getProject(), service);
            if(phpClass == null) {
                return;
            }

            for (Method method : phpClass.getMethods()) {
                if(method.getAccess().isPublic() && !(method.getName().startsWith("__"))) {
                    completionResultSet.addElement(new PhpLookupElement(method));
                }
            }

        }
    }

    public static class DirectoryScopeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, final @NotNull ProcessingContext processingContext, @NotNull final CompletionResultSet completionResultSet) {

            PsiFile originalFile = completionParameters.getOriginalFile();
            if(!Symfony2ProjectComponent.isEnabled(originalFile)) {
                return;
            }

            final PsiDirectory containingDirectory = originalFile.getContainingDirectory();
            if (containingDirectory == null) {
                return;
            }

            final VirtualFile containingDirectoryFiles = containingDirectory.getVirtualFile();
            VfsUtil.visitChildrenRecursively(containingDirectoryFiles, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {

                    String relativePath = VfsUtil.getRelativePath(file, containingDirectoryFiles, '/');
                    if (relativePath == null) {
                        return super.visitFile(file);
                    }

                    completionResultSet.addElement(LookupElementBuilder.create(relativePath).withIcon(file.getFileType().getIcon()));

                    return super.visitFile(file);
                }
            });

        }
    }

    /**
     * Yaml key completion inside services key for class as key shortcut
     *
     * Valid:
     * services:
     *   Foobar<caret>: ~
     *
     * Invalid:
     * services:
     *   Foobar<caret>:
     *      class: Foobar
     */
    private static class MyServiceKeyAsClassCompletionParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
            PsiElement position = parameters.getPosition();

            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            PsiElement serviceDefinition = position.getParent();
            if(serviceDefinition instanceof YAMLKeyValue) {
                YAMLKeyValue aClass = YamlHelper.getYamlKeyValue((YAMLKeyValue) serviceDefinition, "class");
                if(aClass == null) {
                    PhpClassCompletionProvider.addClassCompletion(parameters, completionResultSet, position, false);
                }
            }
        }
    }

    /**
     * services:
     *     _defaults:
     *         bind:
     *             $projectDir: '%kernel.project_dir%'
     */
    private static class NamedArgumentCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            HashSet<String> uniqueParameters = new HashSet<>();

            PsiElement position = parameters.getPosition();
            boolean hasEmptyNextElement = position.getNextSibling() == null;

            ServiceContainerUtil.visitNamedArguments(position.getContainingFile(), pair -> {
                Parameter parameter = pair.getFirst();
                String parameterName = parameter.getName();
                if (uniqueParameters.contains(parameterName)) {
                    return;
                }

                uniqueParameters.add(parameterName);

                // create argument for yaml: $parameter
                result.addElement(
                    LookupElementBuilder.create("$" + parameterName)
                        .withIcon(parameter.getIcon())
                        .withTypeText(StringUtils.stripStart(parameter.getType().toString(), "\\"))
                );

                if (hasEmptyNextElement) {
                    // iterable $handlers => can also provide "!tagged_iterator"
                    if (parameter.getType().getTypes().stream().anyMatch(s -> s.equalsIgnoreCase(PhpType._ITERABLE))) {
                        LookupElementBuilder element = LookupElementBuilder.create("$" + parameterName + ": !tagged_iterator")
                            .withIcon(parameter.getIcon())
                            .withTypeText(StringUtils.stripStart(parameter.getType().toString(), "\\"), true);

                        result.addElement(PrioritizedLookupElement.withPriority(element, -1000));
                    }

                    if (!parameter.getType().getTypes().stream().allMatch(PhpType::isPrimitiveType)) {
                        // $foobar: '@service'
                        result.addAllElements(getServiceSuggestion(position, pair, parameterName, new ContainerCollectionResolver.LazyServiceCollector(position.getProject())));
                    } else {
                        String parameterNormalized = parameterName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                        if (parameterNormalized.length() > 5) {
                            // $projectDir: '%kernel.project_dir%'
                            result.addAllElements(getParameterSuggestion(parameter, parameterName, parameterNormalized));

                            // $kernelClass: '%env(KERNEL_CLASS)%'
                            result.addAllElements(getDotEnvSuggestion(parameter, parameterName, parameterNormalized));
                        }
                    }
                }
            });
        }

        @NotNull
        private Collection<LookupElement> getServiceSuggestion(@NotNull PsiElement position, @NotNull Pair<Parameter, Integer> pair, @NotNull String parameterName, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            Parameter parameter = pair.getFirst();

            PsiElement parameterList = parameter.getParent();
            if (parameterList instanceof ParameterList) {
                PsiElement parent = parameterList.getParent();
                if (parent instanceof Method) {
                    Collection<String> suggestions = new ArrayList<>(ServiceSuggestionUtil.createSuggestions(new ServiceTypeHint(
                        (Method) parent,
                        pair.getSecond(),
                        position
                    ), lazyServiceCollector.getCollector().getServices().values()));

                    return suggestions.stream()
                        .limit(3)
                        .map(service -> {
                            LookupElementBuilder element = LookupElementBuilder.create(String.format("$%s: '@%s'", parameterName, service))
                                .withIcon(Symfony2Icons.SERVICE)
                                .withTypeText(StringUtils.stripStart(parameter.getType().toString(), "\\"), true);

                            return PrioritizedLookupElement.withPriority(element, -1000);
                        })
                        .collect(Collectors.toList());
                }
            }

            return Collections.emptyList();
        }

        /**
         * $projectDir: '%kernel.project_dir%'
         */
        private Collection<LookupElement> getParameterSuggestion(@NotNull Parameter parameter, @NotNull String parameterName, @NotNull String parameterNormalized) {
            Set<String> values = new HashSet<>();

            for (String name : ContainerCollectionResolver.getParameterNames(parameter.getProject())) {
                String symfonyParameterNormalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

                if (symfonyParameterNormalized.contains(parameterNormalized)) {
                    values.add(name);
                }
            }

            // weight items: append all indirect matched, after them in case there they are not similar
            List<String> similarString = new ArrayList<>(SimilarSuggestionUtil.findSimilarString(parameterNormalized, values));
            similarString.addAll(values);

            return similarString.stream()
                .distinct()
                .limit(3)
                .map(service -> {
                    LookupElementBuilder element = LookupElementBuilder.create("$" + parameterName + ": '%" + service + "%'")
                        .withIcon(Symfony2Icons.PARAMETER)
                        .withTypeText(StringUtils.stripStart(parameter.getType().toString(), "\\"), true);

                    return PrioritizedLookupElement.withPriority(element, -1000);
                })
                .collect(Collectors.toList());
        }

        /**
         * "$kernelClass: '%env(KERNEL_CLASS)%'"
         */
        @NotNull
        private Collection<LookupElement> getDotEnvSuggestion(@NotNull Parameter parameter, @NotNull String parameterName, @NotNull String parameterNormalized) {
            Set<String> dotEnv = new HashSet<>();
            for (String name : DotEnvUtil.getEnvironmentVariables(parameter.getProject())) {
                String symfonyParameterNormalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

                if (symfonyParameterNormalized.contains(parameterNormalized)) {
                    dotEnv.add(name);
                }
            }

            // weight items: append all indirect matched, after them in case there they are not similar
            List<String> similarString = new ArrayList<>(SimilarSuggestionUtil.findSimilarString(parameterNormalized, dotEnv));
            similarString.addAll(dotEnv);

            return similarString.stream()
                .distinct()
                .limit(3)
                .map(service -> {
                    LookupElementBuilder element = LookupElementBuilder.create("$" + parameterName + ": '%env(" + service + ")%'")
                        .withIcon(Symfony2Icons.PARAMETER)
                        .withTypeText(StringUtils.stripStart(parameter.getType().toString(), "\\"), true);

                    return PrioritizedLookupElement.withPriority(element, -1000);
                })
                .collect(Collectors.toList());
        }
    }

    /**
     * tags:
     *  - { method: 'foobar' }
     */
    private static class ServiceCallsMethodTestCompletion extends CompletionProvider<CompletionParameters> {

        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();

            String serviceDefinitionClassFromTagMethod = YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement);

            if(serviceDefinitionClassFromTagMethod != null) {
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefinitionClassFromTagMethod);
                if(phpClass != null) {
                    PhpElementsUtil.addClassPublicMethodCompletion(completionResultSet, phpClass);
                }
            }
        }
    }

    private static class ServiceClassMethodInsideScalarKeyCompletion extends CompletionProvider<CompletionParameters> {
        private final String yamlArrayKeyName;

        ServiceClassMethodInsideScalarKeyCompletion(String yamlArrayKeyName) {
            this.yamlArrayKeyName = yamlArrayKeyName;
        }

        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

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

    private static class ServiceCallsMethodCompletion extends CompletionProvider<CompletionParameters> {

        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            // - [ setContainer, [ @service_container ] ]
            PsiElement psiElement = completionParameters.getPosition();

            PsiElement yamlScalar = psiElement.getParent();
            if(yamlScalar instanceof YAMLScalar) {
                YamlHelper.visitServiceCall((YAMLScalar) yamlScalar, clazz -> {
                    PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), clazz);
                    if(phpClass != null) {
                        PhpElementsUtil.addClassPublicMethodCompletion(completionResultSet, phpClass);
                    }
                });
            }
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
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

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

    private static class OrmRelationCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

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

    /**
     * many_to_many:
     *      targetEntity: espend\Doctrine\RelationBundle\Entity\ForeignEntity
     *      joinTable:
     *          name: cms_users_groups
     *           joinColumns:
     *               user_id:
     *                   referencedColumnName: id
     */
    private static class ReferencedColumnCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement position = completionParameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            PsiElement psiElement = PsiTreeUtil.findFirstParent(position, psiElement1 -> {

                if (psiElement1 instanceof YAMLKeyValue) {
                    String s = ((YAMLKeyValue) psiElement1).getKeyText().toLowerCase();
                    if ("joinTable".equalsIgnoreCase(s)) {
                        return true;
                    }
                }

                return false;
            });

            if(psiElement == null) {
                return;
            }

            PsiElement yamlCompoundValue = psiElement.getParent();
            if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
                return;
            }

            String className = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) yamlCompoundValue, "targetEntity", false);
            if(className == null) {
                return;
            }

            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), className);
            if(phpClass == null) {
                return;
            }

            for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                if(field.getRelation() == null) {
                    String columnName = field.getColumn();
                    if(columnName == null) {
                        completionResultSet.addElement(LookupElementBuilder.create(field.getName()).withIcon(Symfony2Icons.DOCTRINE));
                    } else {
                        completionResultSet.addElement(LookupElementBuilder.create(columnName).withTypeText(field.getName(), false).withIcon(Symfony2Icons.DOCTRINE));
                    }
                }
            }

        }
    }

    private static class RouteKeyNameYamlCompletionProvider extends YamlCompletionProvider {

        public RouteKeyNameYamlCompletionProvider(Map<String, String> lookups) {
            super(lookups);
        }

        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

            if(!YamlHelper.isRoutingFile(parameters.getOriginalFile())) {
                return;
            }

            super.addCompletions(parameters, context, resultSet);
        }
    }

    /**
     * "requirements" on "path/pattern: /hello/{name}"
     */
    private static class RouteRequirementsCompletion extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
            YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(completionParameters.getOriginalPosition(), YAMLKeyValue.class);
            if(yamlKeyValue != null) {
                PsiElement compoundValue = yamlKeyValue.getParent();
                if(compoundValue instanceof YAMLCompoundValue) {

                    // path and pattern are valid
                    String pattern = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) compoundValue, "path", false);
                    if(pattern == null) {
                        pattern = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) compoundValue, "pattern", false);
                    }

                    if(pattern != null) {
                        Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(pattern);
                        while(matcher.find()){
                            completionResultSet.addElement(LookupElementBuilder.create(matcher.group(1)));
                        }
                    }

                }
            }
        }
    }
}

