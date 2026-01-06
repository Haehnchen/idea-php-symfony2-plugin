package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.AddRouteAttributeIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for validating PHP attribute scopes (class, method, property).
 *
 * Provides methods to determine if a given PSI element is positioned before
 * a valid PHP attribute target (class, public method, or public field) and
 * whether we should provide attribute completions for that context.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeScopeValidator {

    private static final String AS_TWIG_COMPONENT_ATTRIBUTE_FQN = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";
    private static final String TWIG_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension";
    private static final String AS_TWIG_FILTER_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFilter";
    private static final String AS_TWIG_FUNCTION_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigFunction";
    private static final String AS_TWIG_TEST_ATTRIBUTE_FQN = "\\Twig\\Attribute\\AsTwigTest";
    private static final String DOCTRINE_ENTITY_ATTRIBUTE_FQN = "\\Doctrine\\ORM\\Mapping\\Entity";

    /**
     * Get next element PHP attribute context.
     *
     * @param element The PSI element to check
     * @return PhpClass, Method, Field
     */
    @Nullable
    public static PhpNamedElement getValidAttributeScope(@NotNull PsiElement element) {
        Method method = getMethod(element);
        if (method != null) {
            return method;
        }

        PhpClass phpClass = getPhpClass(element);
        if (phpClass != null) {
            return phpClass;
        }

        return getField(element);
    }

    /**
     * Check if we should provide attribute completions for this element.
     * This checks not just if we're before a class/method/field, but also if that
     * class/method/field is in a context where we provide attribute completions
     * (controller, twig component, twig extension, etc.)
     *
     * @param element The PSI element to check
     * @param project The project
     * @return true if we should provide attribute completions, false otherwise
     */
    public static boolean shouldProvideAttributeCompletions(@NotNull PsiElement element, @NotNull Project project) {
        // Check if we're before a public method
        Method method = getMethod(element);
        if (method != null) {
            PhpClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                // Method-level completions for controller methods
                if (method.getAccess().isPublic() && AddRouteAttributeIntention.isControllerClass(containingClass)) {
                    return true;
                }

                // Method-level completions for Twig extension methods
                if (method.getAccess().isPublic() && isTwigExtensionClass(containingClass)) {
                    return true;
                }

                // Method-level completions for Twig component methods
                if (hasAsTwigComponentAttribute(containingClass)) {
                    return true;
                }

                // Doctrine entity method attributes (lifecycle callbacks)
                if (method.getAccess().isPublic() && PhpAttributeScopeValidator.isDoctrineEntityClass(containingClass)) {
                    return true;
                }
            }
        }

        // Check if we're before a property/field
        Field field = getField(element);
        if (field != null) {
            PhpClass containingClass = field.getContainingClass();
            if (containingClass != null) {
                // Property-level completions for Twig component properties
                if (hasAsTwigComponentAttribute(containingClass)) {
                    return true;
                }

                if (isDoctrineEntityClass(containingClass)) {
                    return true;
                }
            }
        }

        // Check if we're before a class
        PhpClass phpClass = getPhpClass(element);
        if (phpClass != null) {
            // Class-level completions for controller classes
            if (AddRouteAttributeIntention.isControllerClass(phpClass)) {
                return true;
            }

            // Class-level completions for Twig component classes
            if (isTwigComponentClass(project, phpClass)) {
                return true;
            }

            if (isDoctrineEntityClass(phpClass)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds a public method associated with the given element.
     * Returns the method if the element is a child of a method or if the next sibling is a method.
     *
     * @param element The PSI element to check
     * @return The public method if found, null otherwise
     */
    public static @Nullable Method getMethod(@NotNull PsiElement element) {
        if (element.getParent() instanceof Method method) {
            return method;
        } else if (PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true) instanceof Method method) {
            return method;
        }

        return null;
    }

    /**
     * Finds a PhpClass associated with the given element.
     * Returns the class if the element is a child of a class or if the next sibling is a class.
     * Also handles cases where we're in the middle of an attribute list.
     *
     * @param element The PSI element to check
     * @return The PhpClass if found, null otherwise
     */
    public static @Nullable PhpClass getPhpClass(@NotNull PsiElement element) {
        // with a use statement given or non use or namespace:
        // # use App;
        // #<caret>
        // final class Foobar
        PsiElement nextSiblingIgnoreWhitespace = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSiblingIgnoreWhitespace instanceof PhpClass phpClass) {
            return phpClass;
        }

        // #<caret>
        // #[ORM\Table(name: 'foobar')]
        // final class Foobar
        if (nextSiblingIgnoreWhitespace instanceof PhpAttributesList phpAttributesList) {
            PsiElement parent = phpAttributesList.getParent();
            if (parent instanceof PhpClass phpClass) {
                return phpClass;
            }

            return null;
        }

        // #[ORM\Entity]
        // #[ORM\Table(name: 'foobar')]
        // #<caret>
        // final class Foobar
        if (nextSiblingIgnoreWhitespace instanceof LeafPsiElement leafPsiElement) {
            PsiElement parent = leafPsiElement.getParent();
            if (parent instanceof PhpClass phpClass) {
                return phpClass;
            }
        }

        // special via no use statement:
        // namespace App\Entity;
        //
        // #<caret>
        // #[ORM\Entity]
        // #[ORM\Table(name: 'foobar')]
        // class Foobar
        if (nextSiblingIgnoreWhitespace != null && nextSiblingIgnoreWhitespace.getNode().getElementType() == PhpElementTypes.NON_LAZY_GROUP_STATEMENT) {
            if (nextSiblingIgnoreWhitespace.getFirstChild() instanceof PhpClass phpClass) {
                return phpClass;
            }
        }

        return null;
    }

    /**
     * Finds a Field (property) associated with the given element.
     * Returns the field if the element is a child of a field or if the next sibling is a field.
     * Note: Returns fields of any visibility (public, protected, private)
     *
     * @param element The PSI element to check
     * @return The Field if found, null otherwise
     */
    public static @Nullable Field getField(@NotNull PsiElement element) {
        PsiElement nextSiblingIgnoreWhitespace = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        if (nextSiblingIgnoreWhitespace instanceof PhpModifierList phpModifierList) {
            if (phpModifierList.getNextPsiSibling() instanceof Field field) {
                return field;
            }
        }

        if (nextSiblingIgnoreWhitespace instanceof PhpPsiElement phpPsiElement) {
            PhpPsiElement firstPsiChild = phpPsiElement.getFirstPsiChild();
            if (firstPsiChild instanceof PhpModifierList phpModifierList) {
                PhpPsiElement nextPsiSibling = phpModifierList.getNextPsiSibling();

                if (nextPsiSibling instanceof Field field) {
                    return field;
                } else if(nextPsiSibling instanceof PhpFieldType phpFieldType) {
                    PhpPsiElement nextPsiSibling1 = phpFieldType.getNextPsiSibling();
                    if (nextPsiSibling1 instanceof Field field1) {
                        return field1;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if the class has the #[AsTwigComponent] attribute
     */
    private static boolean hasAsTwigComponentAttribute(@NotNull PhpClass phpClass) {
        return !phpClass.getAttributes(AS_TWIG_COMPONENT_ATTRIBUTE_FQN).isEmpty();
    }

    /**
     * Check if the class is a TwigExtension class.
     * A class is considered a TwigExtension if:
     * - Its name ends with "TwigExtension", OR
     * - It extends AbstractExtension or implements ExtensionInterface, OR
     * - Any other public method in the class already has an AsTwig* attribute
     */
    private static boolean isTwigExtensionClass(@NotNull PhpClass phpClass) {
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
            for (PhpAttribute attribute : ownMethod.getAttributes()) {
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
     * Check if the class is a Twig component class.
     * A class is considered a Twig component if:
     * - Its namespace contains "\\Components\\" or ends with "\\Components", OR
     * - There are existing component classes (from index) in the same namespace
     * (e.g., App\Twig\Components\Button, Foo\Components\Form\Input)
     */
    private static boolean isTwigComponentClass(@NotNull Project project, @NotNull PhpClass phpClass) {
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

    public static boolean isDoctrineEntityClass(@NotNull PhpClass phpClass) {
        // Check if the class has the #[Entity] attribute
        if (!phpClass.getAttributes(DOCTRINE_ENTITY_ATTRIBUTE_FQN).isEmpty()) {
            return true;
        }
        
        String fqn = phpClass.getFQN();
        if (fqn.isBlank()) {
            return false;
        }

        return fqn.contains("\\Entity\\");
    }
}
