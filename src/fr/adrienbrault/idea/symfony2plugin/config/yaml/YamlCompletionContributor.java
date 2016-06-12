package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Condition;
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
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.completion.ConfigCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineYamlAnnotationLookupBuilder;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributor extends CompletionContributor {

    private static final Map<String, String> SERVICE_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
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
        put("autowiring_types", ">= 2.8");
        put("deprecated", "(string) >= 2.8");
        put("decorates", null);
        put("decoration_inner_name", null);
        put("decoration_priority", "(int) >= 2.8");
        put("shared", "(bool) >= 3.0");
    }});

    private static final Map<String, String> ROUTE_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("pattern", "deprecated");
        put("defaults", "(bool)");
        put("path", "(string)");
        put("requirements", "(array)");
        put("methods", "(array|string)");
        put("condition", "(string / expression)");
        put("resource", "(string)");
        put("prefix", "(string)");
        put("schemes", "(array|string)");
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

        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getAfterCommaPattern(),
            new FactoryMethodCompletionProvider()
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

        extend(CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("_controller"), new ControllerCompletionProvider());
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

    private static class FactoryMethodCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

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
        protected void addCompletions(@NotNull CompletionParameters completionParameters, final ProcessingContext processingContext, @NotNull final CompletionResultSet completionResultSet) {

            PsiFile originalFile = completionParameters.getOriginalFile();
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
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement position = completionParameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            PsiElement psiElement = PsiTreeUtil.findFirstParent(position, new Condition<PsiElement>() {
                @Override
                public boolean value(PsiElement psiElement) {

                    if (psiElement instanceof YAMLKeyValue) {
                        String s = ((YAMLKeyValue) psiElement).getKeyText().toLowerCase();
                        if ("joinTable".equalsIgnoreCase(s)) {
                            return true;
                        }
                    }

                    return false;
                }
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
    private class RouteRequirementsCompletion extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
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

