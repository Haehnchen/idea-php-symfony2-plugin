package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.AddRouteAttributeIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Provides completion for Symfony PHP attributes like #[Route()] and #[AsController]
 *
 * Triggers when typing "#<caret>" before a public method, class, or property
 *
 * Supports:
 * - Class-level attributes: #[Route], #[AsController], #[IsGranted], #[AsTwigComponent], #[AsCommand]
 * - Method-level attributes: #[Route], #[IsGranted], #[Cache], #[ExposeInTemplate], #[PreMount], #[PostMount]
 * - Property-level attributes: #[ExposeInTemplate]
 * - Twig extension attributes: #[AsTwigFilter], #[AsTwigFunction], #[AsTwigTest]
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeCompletionContributor extends CompletionContributor {

    private static final String ROUTE_ATTRIBUTE_FQN = "\\Symfony\\Component\\Routing\\Attribute\\Route";
    private static final String IS_GRANTED_ATTRIBUTE_FQN = "\\Symfony\\Component\\Security\\Http\\Attribute\\IsGranted";
    private static final String CACHE_ATTRIBUTE_FQN = "\\Symfony\\Component\\HttpKernel\\Attribute\\Cache";
    private static final String AS_CONTROLLER_ATTRIBUTE_FQN = "\\Symfony\\Component\\HttpKernel\\Attribute\\AsController";
    private static final String AS_TWIG_FILTER_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFilter";
    private static final String AS_TWIG_FUNCTION_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFunction";
    private static final String AS_TWIG_TEST_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigTest";
    private static final String AS_TWIG_COMPONENT_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";
    private static final String EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\ExposeInTemplate";
    private static final String PRE_MOUNT_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\PreMount";
    private static final String POST_MOUNT_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\PostMount";
    private static final String TWIG_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension";
    private static final String AS_COMMAND_ATTRIBUTE_FQN = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";
    private static final String COMMAND_CLASS_FQN = "\\Symfony\\Component\\Console\\Command\\Command";
    private static final String INPUT_INTERFACE_FQN = "\\Symfony\\Component\\Console\\Input\\InputInterface";
    private static final String OUTPUT_INTERFACE_FQN = "\\Symfony\\Component\\Console\\Output\\OutputInterface";

    // Doctrine ORM Mapping attributes - Field level
    private static final String DOCTRINE_MAPPING_NAMESPACE = "\\Doctrine\\ORM\\Mapping";
    private static final String DOCTRINE_COLUMN_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Column";
    private static final String DOCTRINE_ID_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Id";
    private static final String DOCTRINE_GENERATED_VALUE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\GeneratedValue";
    private static final String DOCTRINE_ONE_TO_MANY_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\OneToMany";
    private static final String DOCTRINE_ONE_TO_ONE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\OneToOne";
    private static final String DOCTRINE_MANY_TO_ONE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\ManyToOne";
    private static final String DOCTRINE_MANY_TO_MANY_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\ManyToMany";
    private static final String DOCTRINE_JOIN_COLUMN_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\JoinColumn";

    // Doctrine ORM Mapping attributes - Class level
    private static final String DOCTRINE_ENTITY_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Entity";
    private static final String DOCTRINE_TABLE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Table";
    private static final String DOCTRINE_UNIQUE_CONSTRAINT_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\UniqueConstraint";
    private static final String DOCTRINE_INDEX_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Index";
    private static final String DOCTRINE_EMBEDDABLE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Embeddable";
    private static final String DOCTRINE_HAS_LIFECYCLE_CALLBACKS_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\HasLifecycleCallbacks";

    // Doctrine ORM Mapping attributes - Method level (Lifecycle Callbacks)
    private static final String DOCTRINE_POST_LOAD_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PostLoad";
    private static final String DOCTRINE_POST_PERSIST_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PostPersist";
    private static final String DOCTRINE_POST_REMOVE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PostRemove";
    private static final String DOCTRINE_POST_UPDATE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PostUpdate";
    private static final String DOCTRINE_PRE_PERSIST_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PrePersist";
    private static final String DOCTRINE_PRE_REMOVE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PreRemove";
    private static final String DOCTRINE_PRE_UPDATE_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\PreUpdate";

    public PhpAttributeCompletionContributor() {
        // Match any element in PHP files - we'll do more specific checking in the provider
        // Using a broad pattern to catch completion after "#" character
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withLanguage(PhpLanguage.INSTANCE)),
            new PhpAttributeCompletionProvider()
        );
    }

    private static class PhpAttributeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition();
            Project project = position.getProject();

            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            // Check if we're in a context where an attribute makes sense (after "#" with whitespace before it)
            if (!isAttributeContext(parameters)) {
                return;
            }

            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Check if we're before a public method (using shared scope validator)
            PhpNamedElement validAttributeScope = PhpAttributeScopeValidator.getValidAttributeScope(position);
            if (validAttributeScope instanceof Method method) {
                // Method-level attribute completions
                PhpClass containingClass = method.getContainingClass();
                if (containingClass != null) {
                    if (method.getAccess().isPublic() && AddRouteAttributeIntention.isControllerClass(containingClass)) {
                        lookupElements.addAll(getControllerMethodCompletions(project));
                    }

                    if (method.getAccess().isPublic() && isTwigExtensionClass(containingClass)) {
                        lookupElements.addAll(getTwigExtensionCompletions(project));
                    }

                    if (hasAsTwigComponentAttribute(containingClass)) {
                        lookupElements.addAll(getTwigComponentMethodCompletions(project));
                    }

                    // Doctrine entity method attributes (lifecycle callbacks)
                    if (method.getAccess().isPublic() && PhpAttributeScopeValidator.isDoctrineEntityClass(containingClass)) {
                        lookupElements.addAll(getDoctrineMethodAttributeCompletions(project));
                    }
                }
            } else if (validAttributeScope instanceof Field field) {
                // Property-level attribute completions
                PhpClass containingClass = field.getContainingClass();
                if (containingClass != null && hasAsTwigComponentAttribute(containingClass)) {
                    lookupElements.addAll(getTwigComponentPropertyCompletions(project));
                }

                // Doctrine entity field attributes
                if (containingClass != null && PhpAttributeScopeValidator.isDoctrineEntityClass(containingClass)) {
                    lookupElements.addAll(getDoctrineFieldAttributeCompletions(project));
                }
            } else if (validAttributeScope instanceof PhpClass phpClass) {
                // Class-level attribute completions
                if (AddRouteAttributeIntention.isControllerClass(phpClass)) {
                    lookupElements.addAll(getControllerClassCompletions(project));
                }

                if (isTwigComponentClass(project, phpClass)) {
                    lookupElements.addAll(getTwigComponentClassCompletions(project));
                }

                if (isCommandClass(phpClass)) {
                    lookupElements.addAll(getCommandClassCompletions(project));
                }

                // Doctrine entity class attributes
                if (PhpAttributeScopeValidator.isDoctrineEntityClass(phpClass)) {
                    lookupElements.addAll(getDoctrineClassAttributeCompletions(project));
                }
            }

            result.addAllElements(lookupElements);

            // only stop if we are in our hacky scope
            if (validAttributeScope != null) {
                result.stopHere();
            }
        }

        /**
         * Get controller method-level attribute completions (for methods in controller classes)
         */
        private Collection<LookupElement> getControllerMethodCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Route attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_FQN)) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(ROUTE_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(routeLookupElement);
            }

            // Add IsGranted attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, IS_GRANTED_ATTRIBUTE_FQN)) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(IS_GRANTED_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(isGrantedLookupElement);
            }

            // Add Cache attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, CACHE_ATTRIBUTE_FQN)) {
                LookupElement cacheLookupElement = LookupElementBuilder
                    .create("#[Cache]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(CACHE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(CACHE_ATTRIBUTE_FQN, CursorPosition.INSIDE_PARENTHESES));

                lookupElements.add(cacheLookupElement);
            }

            return lookupElements;
        }

        /**
         * Get controller class-level attribute completions (for controller classes)
         */
        private Collection<LookupElement> getControllerClassCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Route attribute completion (for class-level route prefix)
            if (PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_FQN)) {
                LookupElement routeLookupElement = LookupElementBuilder
                    .create("#[Route]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(ROUTE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(ROUTE_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(routeLookupElement);
            }

            // Add AsController attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_CONTROLLER_ATTRIBUTE_FQN)) {
                LookupElement asControllerLookupElement = LookupElementBuilder
                    .create("#[AsController]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_CONTROLLER_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_CONTROLLER_ATTRIBUTE_FQN, CursorPosition.NONE));

                lookupElements.add(asControllerLookupElement);
            }

            // Add IsGranted attribute completion (for class-level security)
            if (PhpElementsUtil.hasClassOrInterface(project, IS_GRANTED_ATTRIBUTE_FQN)) {
                LookupElement isGrantedLookupElement = LookupElementBuilder
                    .create("#[IsGranted]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(IS_GRANTED_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(IS_GRANTED_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(isGrantedLookupElement);
            }

            return lookupElements;
        }

        private Collection<LookupElement> getTwigExtensionCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add AsTwigFilter attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_FILTER_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigFilter]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_FILTER_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_FILTER_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(lookupElement);
            }

            // Add AsTwigFunction attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_FUNCTION_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigFunction]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_FUNCTION_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_FUNCTION_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(lookupElement);
            }

            // Add AsTwigTest attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_TEST_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigTest]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_TEST_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_TEST_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get Twig component class-level attribute completions (for component classes)
         */
        private Collection<LookupElement> getTwigComponentClassCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add AsTwigComponent attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_TWIG_COMPONENT_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsTwigComponent]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_TWIG_COMPONENT_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_TWIG_COMPONENT_ATTRIBUTE_FQN, CursorPosition.NONE));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get command class-level attribute completions (for command classes)
         */
        private Collection<LookupElement> getCommandClassCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add AsCommand attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, AS_COMMAND_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[AsCommand]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(AS_COMMAND_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(AS_COMMAND_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Check if the class is a Twig component class.
         * A class is considered a Twig component if:
         * - Its namespace contains "\\Components\\" or ends with "\\Components", OR
         * - There are existing component classes (from index) in the same namespace
         * (e.g., App\Twig\Components\Button, Foo\Components\Form\Input)
         */
        private boolean isTwigComponentClass(@NotNull Project project, @NotNull PhpClass phpClass) {
            String fqn = phpClass.getFQN();
            if (fqn.isBlank()) {
                return false;
            }

            fqn = StringUtils.stripStart(fqn, "\\");

            int lastBackslash = fqn.lastIndexOf('\\');
            if (lastBackslash == -1) {
                return false; // No namespace
            }

            String namespace = fqn.substring(0, lastBackslash);
            if (namespace.contains("\\Components\\") ||
                namespace.endsWith("\\Components") ||
                namespace.equals("Components")) {
                return true;
            }

            // Check if there are any component classes in the same namespace from the index
            //  keys are FQN class names of components with #[AsTwigComponent] attribute
            for (String key : IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)) {
                String componentFqn = StringUtils.stripStart(key, "\\");

                // Extract namespace from the component FQN
                int componentLastBackslash = componentFqn.lastIndexOf('\\');
                if (componentLastBackslash == -1) {
                    continue;
                }

                // Check if the current class's namespace matches the component namespace
                String componentNamespace = componentFqn.substring(0, componentLastBackslash);
                if (namespace.equals(componentNamespace)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Check if the class is a TwigExtension class.
         * A class is considered a TwigExtension if:
         * - Its name ends with "TwigExtension", OR
         * - It extends AbstractExtension or implements ExtensionInterface, OR
         * - Any other public method in the class already has an AsTwig* attribute
         */
        private boolean isTwigExtensionClass(@NotNull PhpClass phpClass) {
            // Check if the class name ends with "TwigExtension"
            if (phpClass.getName().endsWith("TwigExtension")) {
                return true;
            }

            // Check if the class extends AbstractExtension
            if (PhpElementsUtil.isInstanceOf(phpClass, TWIG_EXTENSION_FQN)) {
                return true;
            }

            // Check if any other public method in the class has an AsTwig* attribute
            for (Method ownMethod : phpClass.getOwnMethods()) {
                if (!ownMethod.getAccess().isPublic() || ownMethod.isStatic()) {
                    continue;
                }

                // Collect attributes once and check for any AsTwig* attribute
                Collection<PhpAttribute> attributes = ownMethod.getAttributes();
                for (PhpAttribute attribute : attributes) {
                    String fqn = attribute.getFQN();
                    if (AS_TWIG_FILTER_ATTRIBUTE_FQN.equals(fqn) ||
                        AS_TWIG_FUNCTION_ATTRIBUTE_FQN.equals(fqn) ||
                        AS_TWIG_TEST_ATTRIBUTE_FQN.equals(fqn)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Get attribute completions for public methods in AsTwigComponent classes
         * Includes: ExposeInTemplate, PreMount, PostMount
         */
        private Collection<LookupElement> getTwigComponentMethodCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add ExposeInTemplate attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[ExposeInTemplate]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN, CursorPosition.NONE));

                lookupElements.add(lookupElement);
            }

            // Add PreMount attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, PRE_MOUNT_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PreMount]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(PRE_MOUNT_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(PRE_MOUNT_ATTRIBUTE_FQN, CursorPosition.NONE));

                lookupElements.add(lookupElement);
            }

            // Add PostMount attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, POST_MOUNT_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PostMount]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(POST_MOUNT_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(POST_MOUNT_ATTRIBUTE_FQN, CursorPosition.NONE));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get attribute completions for properties in AsTwigComponent classes
         * Includes: ExposeInTemplate
         */
        private Collection<LookupElement> getTwigComponentPropertyCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add ExposeInTemplate attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[ExposeInTemplate]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpAttributeInsertHandler(EXPOSE_IN_TEMPLATE_ATTRIBUTE_FQN, CursorPosition.INSIDE_QUOTES))
;

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get attribute completions for properties in Doctrine entity classes
         * Includes: Column, Id, GeneratedValue, OneToMany, OneToOne, ManyToOne, ManyToMany, JoinColumn
         */
        private Collection<LookupElement> getDoctrineFieldAttributeCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Column attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_COLUMN_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Column]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_COLUMN_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_COLUMN_ATTRIBUTE_FQN, "Column"));

                lookupElements.add(lookupElement);
            }

            // Add Id attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_ID_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Id]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_ID_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_ID_ATTRIBUTE_FQN, "Id"));

                lookupElements.add(lookupElement);
            }

            // Add GeneratedValue attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_GENERATED_VALUE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[GeneratedValue]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_GENERATED_VALUE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_GENERATED_VALUE_ATTRIBUTE_FQN, "GeneratedValue"));

                lookupElements.add(lookupElement);
            }

            // Add OneToMany attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_ONE_TO_MANY_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[OneToMany]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_ONE_TO_MANY_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_ONE_TO_MANY_ATTRIBUTE_FQN, "OneToMany"));

                lookupElements.add(lookupElement);
            }

            // Add OneToOne attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_ONE_TO_ONE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[OneToOne]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_ONE_TO_ONE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_ONE_TO_ONE_ATTRIBUTE_FQN, "OneToOne"));

                lookupElements.add(lookupElement);
            }

            // Add ManyToOne attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_MANY_TO_ONE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[ManyToOne]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_MANY_TO_ONE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_MANY_TO_ONE_ATTRIBUTE_FQN, "ManyToOne"));

                lookupElements.add(lookupElement);
            }

            // Add ManyToMany attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_MANY_TO_MANY_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[ManyToMany]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_MANY_TO_MANY_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_MANY_TO_MANY_ATTRIBUTE_FQN, "ManyToMany"));

                lookupElements.add(lookupElement);
            }

            // Add JoinColumn attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_JOIN_COLUMN_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[JoinColumn]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_JOIN_COLUMN_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_JOIN_COLUMN_ATTRIBUTE_FQN, "JoinColumn"));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get attribute completions for Doctrine entity classes
         * Includes: Entity, Table, UniqueConstraint, Index, Embeddable, HasLifecycleCallbacks
         */
        private Collection<LookupElement> getDoctrineClassAttributeCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add Entity attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_ENTITY_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Entity]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_ENTITY_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_ENTITY_ATTRIBUTE_FQN, "Entity"));

                lookupElements.add(lookupElement);
            }

            // Add Table attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_TABLE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Table]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_TABLE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_TABLE_ATTRIBUTE_FQN, "Table"));

                lookupElements.add(lookupElement);
            }

            // Add UniqueConstraint attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_UNIQUE_CONSTRAINT_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[UniqueConstraint]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_UNIQUE_CONSTRAINT_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_UNIQUE_CONSTRAINT_ATTRIBUTE_FQN, "UniqueConstraint"));

                lookupElements.add(lookupElement);
            }

            // Add Index attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_INDEX_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Index]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_INDEX_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_INDEX_ATTRIBUTE_FQN, "Index"));

                lookupElements.add(lookupElement);
            }

            // Add Embeddable attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_EMBEDDABLE_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[Embeddable]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_EMBEDDABLE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_EMBEDDABLE_ATTRIBUTE_FQN, "Embeddable"));

                lookupElements.add(lookupElement);
            }

            // Add HasLifecycleCallbacks attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_HAS_LIFECYCLE_CALLBACKS_ATTRIBUTE_FQN)) {
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[HasLifecycleCallbacks]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_HAS_LIFECYCLE_CALLBACKS_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineAttributeInsertHandler(DOCTRINE_HAS_LIFECYCLE_CALLBACKS_ATTRIBUTE_FQN, "HasLifecycleCallbacks"));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Get attribute completions for public methods in Doctrine entity classes (Lifecycle Callbacks)
         * Includes: PostLoad, PostPersist, PostRemove, PostUpdate, PrePersist, PreRemove, PreUpdate
         *
         * These attributes use a special insert handler that also adds #[HasLifecycleCallbacks]
         * to the class if not already present.
         */
        private Collection<LookupElement> getDoctrineMethodAttributeCompletions(@NotNull Project project) {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // Add PostLoad attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_POST_LOAD_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_POST_LOAD_ATTRIBUTE_FQN, "PostLoad");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PostLoad]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_POST_LOAD_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_POST_LOAD_ATTRIBUTE_FQN, "PostLoad", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PostPersist attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_POST_PERSIST_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_POST_PERSIST_ATTRIBUTE_FQN, "PostPersist");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PostPersist]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_POST_PERSIST_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_POST_PERSIST_ATTRIBUTE_FQN, "PostPersist", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PostRemove attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_POST_REMOVE_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_POST_REMOVE_ATTRIBUTE_FQN, "PostRemove");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PostRemove]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_POST_REMOVE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_POST_REMOVE_ATTRIBUTE_FQN, "PostRemove", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PostUpdate attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_POST_UPDATE_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_POST_UPDATE_ATTRIBUTE_FQN, "PostUpdate");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PostUpdate]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_POST_UPDATE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_POST_UPDATE_ATTRIBUTE_FQN, "PostUpdate", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PrePersist attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_PRE_PERSIST_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_PRE_PERSIST_ATTRIBUTE_FQN, "PrePersist");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PrePersist]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_PRE_PERSIST_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_PRE_PERSIST_ATTRIBUTE_FQN, "PrePersist", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PreRemove attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_PRE_REMOVE_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_PRE_REMOVE_ATTRIBUTE_FQN, "PreRemove");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PreRemove]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_PRE_REMOVE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_PRE_REMOVE_ATTRIBUTE_FQN, "PreRemove", baseHandler));

                lookupElements.add(lookupElement);
            }

            // Add PreUpdate attribute completion
            if (PhpElementsUtil.hasClassOrInterface(project, DOCTRINE_PRE_UPDATE_ATTRIBUTE_FQN)) {
                PhpDoctrineAttributeInsertHandler baseHandler = new PhpDoctrineAttributeInsertHandler(DOCTRINE_PRE_UPDATE_ATTRIBUTE_FQN, "PreUpdate");
                LookupElement lookupElement = LookupElementBuilder
                    .create("#[PreUpdate]")
                    .withIcon(Symfony2Icons.SYMFONY_ATTRIBUTE)
                    .withTypeText(StringUtils.stripStart(DOCTRINE_PRE_UPDATE_ATTRIBUTE_FQN, "\\"), true)
                    .withInsertHandler(new PhpDoctrineLifecycleAttributeInsertHandler(DOCTRINE_PRE_UPDATE_ATTRIBUTE_FQN, "PreUpdate", baseHandler));

                lookupElements.add(lookupElement);
            }

            return lookupElements;
        }

        /**
         * Check if the class has the #[AsTwigComponent] attribute
         */
        private boolean hasAsTwigComponentAttribute(@NotNull PhpClass phpClass) {
            return !phpClass.getAttributes(AS_TWIG_COMPONENT_ATTRIBUTE_FQN).isEmpty();
        }

        /**
         * Check if the class is a valid command class for AsCommand attribute.
         * A class is considered a valid command if:
         * - Its name ends with "Command", OR
         * - Its namespace contains "\\Command\\", OR
         * - It extends Symfony's Command class, OR
         * - It has an __invoke method with Input or Output interface parameters (Symfony 7.4 style)
         */
        private boolean isCommandClass(@NotNull PhpClass phpClass) {
            // Check if the class name ends with "Command"
            if (phpClass.getName().endsWith("Command")) {
                return true;
            }

            // Check if the class is in a Command namespace
            String fqn = phpClass.getFQN();
            if (!fqn.isBlank() && fqn.contains("\\Command\\")) {
                return true;
            }

            // Check if the class extends Symfony's Command class
            if (PhpElementsUtil.isInstanceOf(phpClass, COMMAND_CLASS_FQN)) {
                return true;
            }

            // Check if the class has an __invoke method with Input or Output interface parameters
            Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
            if (invokeMethod != null) {
                for (Parameter parameter : invokeMethod.getParameters()) {
                    // Check via PhpType resolution for exact interface match
                    PhpType type = parameter.getType();
                    for (String typeString : type.getTypes()) {
                        // Strip leading backslash for comparison
                        String normalizedType = StringUtils.stripStart(typeString, "\\");

                        if (normalizedType.equals(StringUtils.stripStart(INPUT_INTERFACE_FQN, "\\")) ||
                            normalizedType.equals(StringUtils.stripStart(OUTPUT_INTERFACE_FQN, "\\"))) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Check if we're in a context where typing "#" for attributes makes sense
         * (i.e., after "#" character with whitespace before it)
         */
        private boolean isAttributeContext(@NotNull CompletionParameters parameters) {
            int offset = parameters.getOffset();
            PsiFile psiFile = parameters.getOriginalFile();

            // Need at least 2 characters before cursor to check for "# " pattern
            if (offset < 2) {
                return false;
            }

            // Check if there's a "#" before the cursor with whitespace before it
            // secure length check
            CharSequence documentText = parameters.getEditor().getDocument().getCharsSequence();
            if (offset < documentText.length()) {
                return documentText.charAt(offset - 1) == '#' && psiFile.findElementAt(offset - 2) instanceof PsiWhiteSpace;
            }

            return false;
        }
    }

    /**
     * Enum to specify where the cursor should be positioned after attribute insertion
     */
    private enum CursorPosition {
        /**
         * Position cursor inside quotes: #[Attribute("<caret>")]
         */
        INSIDE_QUOTES,
        /**
         * Position cursor inside parentheses: #[Attribute(<caret>)]
         */
        INSIDE_PARENTHESES,
        /**
         * No parentheses needed: #[Attribute]<caret>
         */
        NONE
    }


    /**
     * Insert handler that adds a PHP attribute
     */
    private record PhpAttributeInsertHandler(@NotNull String attributeFqn, @NotNull CursorPosition cursorPosition) implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Editor editor = context.getEditor();
            Document document = editor.getDocument();
            Project project = context.getProject();

            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();

            // IMPORTANT: Find the target class/method BEFORE modifying the document
            // because PSI structure might change after deletions
            PsiFile file = context.getFile();
            PsiElement originalElement = file.findElementAt(startOffset);
            if (originalElement == null) {
                return;
            }

            // Determine the target context (method, field, or class) dynamically using shared scope validator
            PhpNamedElement validAttributeScope = PhpAttributeScopeValidator.getValidAttributeScope(originalElement);
            if (validAttributeScope == null) {
                return;
            }

            // Find and delete the "#" before the completion position to avoid "##[Attribute()]"
            // Check the 1-2 positions immediately before startOffset
            CharSequence text = document.getCharsSequence();
            int deleteStart = startOffset;

            // Check startOffset - 1 and startOffset - 2 for the "#" character
            if (startOffset > 0 && text.charAt(startOffset - 1) == '#') {
                deleteStart = startOffset - 1;
            } else if (startOffset > 1 && text.charAt(startOffset - 2) == '#') {
                // Handle case where there might be a single whitespace between # and dummy identifier
                deleteStart = startOffset - 2;
            }

            // Delete from the "#" (or startOffset if no "#" found) to tailOffset
            document.deleteString(deleteStart, tailOffset);

            // Store the original insertion offset (where user typed "#")
            int originalInsertionOffset = deleteStart;

            // Commit after deletion
            PsiDocumentManager.getInstance(project).commitDocument(document);

            // Extract class name from FQN (get the last part after the last backslash)
            String className = attributeFqn.substring(attributeFqn.lastIndexOf('\\') + 1);

            // Store document length before adding import to calculate offset shift
            int documentLengthBeforeImport = document.getTextLength();

            // Add import if necessary - this will modify the document!
            String importedName = PhpElementsUtil.insertUseIfNecessary(validAttributeScope, attributeFqn);
            if (importedName != null) {
                className = importedName;
            }

            // IMPORTANT: After adding import, commit and recalculate the insertion position
            PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Calculate how much the document length changed (import adds characters above our insertion point)
            int documentLengthAfterImport = document.getTextLength();
            int offsetShift = documentLengthAfterImport - documentLengthBeforeImport;

            // Adjust insertion offset by the shift caused by import
            int currentInsertionOffset = originalInsertionOffset + offsetShift;

            // Check if there's already a newline at the current position
            // to avoid adding double newlines
            CharSequence currentText = document.getCharsSequence();
            boolean hasNewlineAfter = false;
            if (currentInsertionOffset < currentText.length()) {
                char nextChar = currentText.charAt(currentInsertionOffset);
                hasNewlineAfter = (nextChar == '\n' || nextChar == '\r');
            }

            // Build attribute text based on cursor position
            String attributeText;
            String newline = hasNewlineAfter ? "" : "\n";

            if (cursorPosition == CursorPosition.INSIDE_QUOTES) {
                attributeText = "#[" + className + "(\"\")]" + newline;
            } else if (cursorPosition == CursorPosition.INSIDE_PARENTHESES) {
                attributeText = "#[" + className + "()]" + newline;
            } else {
                // CursorPosition.NONE - no parentheses
                attributeText = "#[" + className + "]" + newline;
            }

            // Insert at the cursor position where user typed "#"
            document.insertString(currentInsertionOffset, attributeText);

            // Commit and reformat
            psiDocManager.commitDocument(document);
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);

            // Reformat the added attribute
            CodeUtil.reformatAddedAttribute(project, document, currentInsertionOffset);

            // After reformatting, position cursor based on the cursor position mode
            psiDocManager.commitDocument(document);

            // Get fresh PSI and find the attribute we just added
            PsiFile finalFile = psiDocManager.getPsiFile(document);
            if (finalFile != null) {
                // Look for element INSIDE the inserted attribute (a few chars after insertion point)
                PsiElement elementInsideAttribute = finalFile.findElementAt(currentInsertionOffset + 3);
                if (elementInsideAttribute != null) {
                    // Find the PhpAttribute element
                    PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(elementInsideAttribute, PhpAttribute.class);

                    if (phpAttribute != null) {
                        int attributeStart = phpAttribute.getTextRange().getStartOffset();
                        int attributeEnd = phpAttribute.getTextRange().getEndOffset();
                        CharSequence attributeContent = document.getCharsSequence().subSequence(attributeStart, attributeEnd);

                        if (cursorPosition == CursorPosition.NONE) {
                            // For attributes without parentheses, position cursor at the end of the line
                            // (after the closing bracket and newline)
                            editor.getCaretModel().moveToOffset(attributeEnd + 1);
                        } else {
                            // Find cursor position based on mode
                            String searchChar = cursorPosition == CursorPosition.INSIDE_QUOTES ? "\"" : "(";
                            int searchIndex = attributeContent.toString().indexOf(searchChar);

                            if (searchIndex >= 0) {
                                // Position cursor right after the search character
                                int caretOffset = attributeStart + searchIndex + 1;
                                editor.getCaretModel().moveToOffset(caretOffset);
                            }
                        }
                    }
                }
            }
        }
    }
}
