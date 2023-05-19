package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionScalarArgument;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyImplicitUsageProvider implements ImplicitUsageProvider {
    private static final String[] ROUTE_ANNOTATIONS = new String[] {
        "\\Symfony\\Component\\Routing\\Annotation\\Route",
        "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route"
    };

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof Method method && method.getAccess() == PhpModifier.Access.PUBLIC) {
            return isMethodARoute(method)
                || isSubscribedEvent(method)
                || isAsEventListenerMethodPhpAttribute(method);
        } else if (element instanceof PhpClass phpClass) {
            return isRouteClass(phpClass)
                || isCommandAndService(phpClass)
                || isSubscribedEvent(phpClass)
                || isVoter(phpClass)
                || isTwigExtension(phpClass)
                || isEntityRepository(phpClass)
                || isConstraint(phpClass)
                || isKernelEventListener(phpClass);
        }

        return false;
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
        return PhpElementsUtil.getClassesInterface(phpClass.getProject(), className).size() > 0;
    }

    private boolean isEntityRepository(@NotNull PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\ORM\\EntityRepository")
            && DoctrineMetadataUtil.findMetadataForRepositoryClass(phpClass).size() > 0;
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
        if (phpDocCommentAnnotationContainer != null && phpDocCommentAnnotationContainer.getFirstPhpDocBlock(ROUTE_ANNOTATIONS) != null) {
            return true;
        }

        for (String route : ROUTE_ANNOTATIONS) {
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
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        for (PhpAttribute attribute : containingClass.getAttributes("\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener")) {
            for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                if ("method".equals(argument.getName()) && argument.getArgument() instanceof PhpExpectedFunctionScalarArgument scalarArgument) {
                    String value = PsiElementUtils.trimQuote(scalarArgument.getNormalizedValue());

                    if (method.getName().equals(value)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
