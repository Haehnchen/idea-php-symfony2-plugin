package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionScalarArgument;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeScopeValidator;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyImplicitUsageProvider implements ImplicitUsageProvider {
    private static final Set<String> CLASS_LEVEL_IMPLICIT_USAGE_ATTRIBUTES = Set.of(
        "\\Symfony\\Component\\Console\\Attribute\\AsCommand",
        "\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener",
        "\\Symfony\\Component\\Messenger\\Attribute\\AsMessageHandler",
        "\\Symfony\\Component\\Scheduler\\Attribute\\AsSchedule",
        "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent",
        "\\Doctrine\\Bundle\\DoctrineBundle\\Attribute\\AsDoctrineListener",
        "\\Doctrine\\Bundle\\DoctrineBundle\\Attribute\\AsEntityListener",
        "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutoconfigureTag",
        "\\Symfony\\Component\\Validator\\Constraints\\Callback",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsAnnounceListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsCompletedListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsEnterListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsEnteredListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsGuardListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsLeaveListener",
        "\\Symfony\\Component\\Workflow\\Attribute\\AsTransitionListener"
    );
    private static final Set<String> DOCTRINE_LIFECYCLE_METHOD_ATTRIBUTES = Set.of(
        "\\Doctrine\\ORM\\Mapping\\PrePersist",
        "\\Doctrine\\ORM\\Mapping\\PostPersist",
        "\\Doctrine\\ORM\\Mapping\\PreUpdate",
        "\\Doctrine\\ORM\\Mapping\\PostUpdate",
        "\\Doctrine\\ORM\\Mapping\\PreRemove",
        "\\Doctrine\\ORM\\Mapping\\PostRemove",
        "\\Doctrine\\ORM\\Mapping\\PostLoad",
        "\\Doctrine\\ORM\\Mapping\\PreFlush"
    );
    private static final Set<String> MCP_CAPABILITY_ATTRIBUTES = Set.of(
        "\\Mcp\\Capability\\Attribute\\McpTool",
        "\\Mcp\\Capability\\Attribute\\McpPrompt",
        "\\Mcp\\Capability\\Attribute\\McpResource",
        "\\Mcp\\Capability\\Attribute\\McpResourceTemplate"
    );

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof Method method && method.getAccess() == PhpModifier.Access.PUBLIC) {
            return isMethodARoute(method)
                || isSubscribedEvent(method)
                || isAsEventListenerMethodPhpAttribute(method)
                || isAsEntityListenerMethodPhpAttribute(method)
                || isAssertCallbackMethod(method)
                || isDoctrineLifecycleCallbackMethod(method)
                || isMcpCapabilityMethod(method)
                || hasTwigAttribute(method);
        } else if (element instanceof PhpClass phpClass) {
            return isRouteClass(phpClass)
                || isCommandAndService(phpClass)
                || isSubscribedEvent(phpClass)
                || isVoter(phpClass)
                || isTwigExtension(phpClass)
                || isEntityRepository(phpClass)
                || isConstraint(phpClass)
                || isKernelEventListener(phpClass)
                || isMcpCapabilityClass(phpClass)
                || hasClassLevelImplicitUsageAttribute(phpClass);
        }

        return false;
    }

    private boolean isDoctrineLifecycleCallbackMethod(@NotNull Method method) {
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null || !isDoctrineEntity(containingClass)) {
            return false;
        }

        return DOCTRINE_LIFECYCLE_METHOD_ATTRIBUTES.stream().anyMatch(attribute -> !method.getAttributes(attribute).isEmpty());
    }

    private boolean isDoctrineEntity(@NotNull PhpClass phpClass) {
        return PhpAttributeScopeValidator.isDoctrineEntityClass(phpClass);
    }

    private boolean isMcpCapabilityClass(@NotNull PhpClass phpClass) {
        return hasMcpCapabilityAttribute(phpClass);
    }

    private boolean isMcpCapabilityMethod(@NotNull Method method) {
        return hasOwnMcpCapabilityAttribute(method)
            || "__invoke".equals(method.getName()) && hasMcpCapabilityAttribute(method.getContainingClass());
    }

    private boolean hasMcpCapabilityAttribute(@Nullable PhpClass phpClass) {
        return phpClass != null && MCP_CAPABILITY_ATTRIBUTES.stream().anyMatch(attribute -> !phpClass.getAttributes(attribute).isEmpty());
    }

    private boolean hasOwnMcpCapabilityAttribute(@NotNull Method method) {
        return MCP_CAPABILITY_ATTRIBUTES.stream().anyMatch(attribute -> !method.getAttributes(attribute).isEmpty());
    }

    private boolean hasClassLevelImplicitUsageAttribute(@NotNull PhpClass phpClass) {
        return CLASS_LEVEL_IMPLICIT_USAGE_ATTRIBUTES.stream().anyMatch(attribute -> !phpClass.getAttributes(attribute).isEmpty());
    }

    private boolean isKernelEventListener(@NotNull PhpClass phpClass) {
        if (!ServiceUtil.isPhpClassAService(phpClass)) {
            return false;
        }

        return ServiceUtil.isPhpClassTaggedWith(phpClass, "kernel.event_listener");
    }

    private boolean isConstraint(@NotNull PhpClass phpClass) {
        if(!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Validator\\Constraint")) {
            return false;
        }

        // class in same namespace
        // @TODO: validateBy alias
        String className = phpClass.getFQN() + "Validator";
        return !PhpElementsUtil.getClassesInterface(phpClass.getProject(), className).isEmpty();
    }

    private boolean isEntityRepository(@NotNull PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\ORM\\EntityRepository")
            && !DoctrineMetadataUtil.findMetadataForRepositoryClass(phpClass).isEmpty();
    }

    private boolean isTwigExtension(PhpClass phpClass) {
        if ((PhpElementsUtil.isInstanceOf(phpClass, "\\Twig\\Extension\\ExtensionInterface") || PhpElementsUtil.isInstanceOf(phpClass,"\\Twig_ExtensionInterface")) && ServiceUtil.isPhpClassAService(phpClass)) {
            Set<String> methods = new HashSet<>();

            Collection<PhpClass> phpClasses = new HashSet<>() {{
                addAll(PhpElementsUtil.getClassesInterface(phpClass.getProject(), "\\Twig\\Extension\\ExtensionInterface"));
                addAll(PhpElementsUtil.getClassesInterface(phpClass.getProject(), "\\Twig_ExtensionInterface"));
            }};

            for (PhpClass aClass : phpClasses) {
                methods.addAll(aClass.getMethods()
                    .stream()
                    .filter(method -> !method.isStatic() && method.getAccess() == PhpModifier.Access.PUBLIC).map(PhpNamedElement::getName)
                    .collect(Collectors.toSet())
                );
            }

            return Arrays.stream(phpClass.getOwnMethods())
                .anyMatch(ownMethod -> ownMethod.getAccess() == PhpModifier.Access.PUBLIC && methods.contains(ownMethod.getName()));
        }

        return false;
    }

    private boolean isVoter(PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Security\\Core\\Authorization\\Voter\\VoterInterface")
            && ServiceUtil.isPhpClassAService(phpClass);
    }

    private boolean isRouteClass(@NotNull PhpClass phpClass) {
        return phpClass.getMethods()
            .stream()
            .filter(method -> method.getAccess() == PhpModifier.Access.PUBLIC)
            .anyMatch(this::isMethodARoute);
    }

    private boolean isCommandAndService(PhpClass element) {
        return PhpElementsUtil.isInstanceOf(element, "\\Symfony\\Component\\Console\\Command\\Command")
            && ServiceUtil.isPhpClassAService(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }

    private boolean isMethodARoute(@NotNull Method method) {
        PhpDocCommentAnnotation phpDocCommentAnnotationContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(method.getDocComment());
        if (phpDocCommentAnnotationContainer != null && phpDocCommentAnnotationContainer.getFirstPhpDocBlock(RouteHelper.ROUTE_ANNOTATIONS) != null) {
            return true;
        }

        for (String route : RouteHelper.ROUTE_ANNOTATIONS) {
            Collection<@NotNull PhpAttribute> attributes = method.getAttributes(route);
            if (!attributes.isEmpty()) {
                return true;
            }
        }

        return RouteHelper.isRouteExistingForMethod(method);
    }

    private boolean isSubscribedEvent(@NotNull PhpClass phpClass) {
        return phpClass.getMethods()
            .stream()
            .filter(method -> method.getAccess() == PhpModifier.Access.PUBLIC)
            .anyMatch(this::isSubscribedEvent);
    }

    private boolean isSubscribedEvent(@NotNull Method method) {
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null || !PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface")) {
            return false;
        }

        Method subscribedEvents = containingClass.findMethodByName("getSubscribedEvents");
        if (subscribedEvents == null) {
            return false;
        }

        for (PsiElement returnArgument : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(subscribedEvents)) {
            PsiElement[] psiElements = PsiTreeUtil.collectElements(returnArgument, element -> {
                if (!(element instanceof StringLiteralExpression)) {
                    return false;
                }

                PsiElement parent = element.getParent();
                return parent != null && parent.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE && parent.getChildren().length == 1;
            });

            for (PsiElement psiElement : psiElements) {
                if (psiElement instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) psiElement).getContents();
                    if (StringUtils.isNotBlank(contents) && contents.equalsIgnoreCase(method.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isAsEventListenerMethodPhpAttribute(@NotNull Method method) {
        if (!method.getAttributes("\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener").isEmpty()) {
            return true;
        }

        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        for (PhpAttribute attribute : containingClass.getAttributes("\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener")) {
            String methodAttr = null;
            String eventAttr = null;
            for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                if ("method".equals(argument.getName())) {
                    if (argument.getArgument() instanceof PhpExpectedFunctionScalarArgument scalarArgument) {
                        methodAttr = PsiElementUtils.trimQuote(scalarArgument.getNormalizedValue());

                        if (method.getName().equals(methodAttr)) {
                            return true;
                        }
                    }
                } else if ("event".equals(argument.getName())) {
                    if (argument.getArgument() instanceof PhpExpectedFunctionScalarArgument scalarArgument) {
                        eventAttr = PsiElementUtils.trimQuote(scalarArgument.getNormalizedValue());
                    }
                }
            }

            if (eventAttr == null && methodAttr == null) {
                methodAttr = "__invoke";
            }

            if (methodAttr == null) {
                String snakeCased = Pattern.compile("(?<=\\b|_)[a-z]", Pattern.CASE_INSENSITIVE)
                        .matcher(eventAttr)
                        .replaceAll(matchResult -> matchResult.group().toUpperCase());

                methodAttr = "on" + Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE)
                        .matcher(snakeCased)
                        .replaceAll("");
            }

            if (method.getName().equals(methodAttr)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAssertCallbackMethod(@NotNull Method method) {
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        for (PhpAttribute attribute : containingClass.getAttributes("\\Symfony\\Component\\Validator\\Constraints\\Callback")) {
            String callback = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "callback");
            if (method.getName().equals(callback)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAsEntityListenerMethodPhpAttribute(@NotNull Method method) {
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        for (PhpAttribute attribute : containingClass.getAttributes("\\Doctrine\\Bundle\\DoctrineBundle\\Attribute\\AsEntityListener")) {
            String callback = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, "method");
            if (method.getName().equals(callback)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasTwigAttribute(@NotNull Method method) {
        return !method.getAttributes("\\Twig\\Attribute\\AsTwigFilter").isEmpty()
            || !method.getAttributes("\\Twig\\Attribute\\AsTwigFunction").isEmpty()
            || !method.getAttributes("\\Twig\\Attribute\\AsTwigTest").isEmpty();
    }
}
