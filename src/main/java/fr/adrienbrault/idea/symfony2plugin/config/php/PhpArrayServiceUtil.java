package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilities for Symfony service definitions inside PHP `services` arrays.
 *
 * Supported roots:
 * `return ['services' => [...]]`
 * `App::config(['services' => [...]])`
 *
 * Supported service ids:
 * `'app.mailer'`
 * `Mailer::class`
 */
public final class PhpArrayServiceUtil {
    private PhpArrayServiceUtil() {
    }

    /**
     * Checks whether an element belongs to a PHP array-based services config.
     */
    public static boolean isInsidePhpArrayServiceConfig(@NotNull PsiElement element) {
        return getServiceEntry(element) != null;
    }

    /**
     * Returns the direct hash key for a value inside a service definition.
     */
    @Nullable
    public static String getParentHashKey(@NotNull PsiElement valueElement) {
        ArrayHashElement arrayHashElement = PsiTreeUtil.getParentOfType(valueElement, ArrayHashElement.class);
        if (arrayHashElement == null || arrayHashElement.getValue() == null || !PsiTreeUtil.isAncestor(arrayHashElement.getValue(), valueElement, false)) {
            return null;
        }

        return getArrayKey(arrayHashElement.getKey());
    }

    /**
     * Checks whether a PSI element is the key of a service entry under `services`.
     */
    public static boolean isServiceKey(@NotNull PsiElement keyElement) {
        PsiElement parentScope = getImmediateArrayScope(keyElement);
        if (!(parentScope instanceof ArrayHashElement serviceEntry)) {
            return false;
        }

        return serviceEntry.getKey() == keyElement && isServiceEntry(serviceEntry);
    }

    /**
     * Returns the direct service attribute name for values like `'decorates' => ...`.
     */
    @Nullable
    public static String getServiceAttributeName(@NotNull PsiElement valueElement) {
        ArrayHashElement attributeEntry = getDirectArrayHashElement(valueElement);
        if (attributeEntry == null || attributeEntry.getValue() != valueElement) {
            return null;
        }

        PsiElement parentScope = getImmediateArrayScope(attributeEntry);
        if (!(parentScope instanceof ArrayCreationExpression serviceDefinition)) {
            return null;
        }

        ArrayHashElement serviceEntry = getDirectArrayHashElement(serviceDefinition);
        if (serviceEntry == null || !isServiceEntry(serviceEntry)) {
            return null;
        }

        return getArrayKey(attributeEntry.getKey());
    }

    /**
     * Builds the nested path from a service definition root to a nested value.
     *
     * Examples:
     * `'decorates' => 'mailer'` => `["decorates"]`
     * `'arguments' => ['@logger']` => `["arguments", "0"]`
     * `'calls' => [['setLogger', ['@logger']]]` => `["calls", "0", "1", "0"]`
     * `'tags' => [['name' => 'kernel.event_listener']]` => `["tags", "0", "name"]`
     *
     * @param valueElement the value PSI element inside a PHP `services` array, such as a `StringLiteralExpression` or `ClassConstantReference`
     */
    @Nullable
    public static ServiceConfigPath getKeyPath(@NotNull PsiElement valueElement) {
        ArrayHashElement serviceEntry = getServiceEntry(valueElement);
        if (serviceEntry == null) {
            return null;
        }

        List<String> path = new ArrayList<>();
        PsiElement current = valueElement;

        while (true) {
            ArrayCreationExpression parentArray = getParentArrayForValue(current);
            if (parentArray != null) {
                path.add(getArrayValueIndex(current, parentArray));
                current = parentArray;
                continue;
            }

            ArrayHashElement arrayHashElement = getDirectArrayHashElement(current);
            if (arrayHashElement == null || arrayHashElement.getValue() == null || !PsiTreeUtil.isAncestor(arrayHashElement.getValue(), current, false)) {
                return null;
            }

            if (arrayHashElement == serviceEntry) {
                break;
            }

            PsiElement parentScope = getImmediateArrayScope(arrayHashElement);
            if (!(parentScope instanceof ArrayCreationExpression arrayCreationExpression)) {
                return null;
            }

            path.add(getArrayHashSegment(arrayHashElement));
            current = arrayCreationExpression;
        }

        Collections.reverse(path);

        return new ServiceConfigPath(path.toArray(new String[0]));
    }

    /**
     * Checks whether a scalar service value is an alias shortcut like '@service'.
     */
    public static boolean isServiceLevelAliasValue(@NotNull StringLiteralExpression value) {
        ArrayHashElement serviceEntry = getServiceEntry(value);
        if (serviceEntry == null || serviceEntry.getValue() != value) {
            return false;
        }

        String contents = value.getContents();
        return StringUtils.isNotBlank(contents) && contents.startsWith("@") && contents.length() > 1;
    }

    /**
     * Finds the surrounding service entry inside the top-level services map.
     */
    @Nullable
    public static ArrayHashElement getServiceEntry(@NotNull PsiElement element) {
        PsiElement current = element;

        while (true) {
            ArrayCreationExpression parentArray = getParentArrayForValue(current);
            if (parentArray != null) {
                current = parentArray;
                continue;
            }

            ArrayHashElement arrayHashElement = getDirectArrayHashElement(current);
            if (arrayHashElement == null || arrayHashElement.getValue() == null || !PsiTreeUtil.isAncestor(arrayHashElement.getValue(), current, false)) {
                return null;
            }

            if (isServiceEntry(arrayHashElement)) {
                return arrayHashElement;
            }

            PsiElement parentScope = getImmediateArrayScope(arrayHashElement);
            if (!(parentScope instanceof ArrayCreationExpression arrayCreationExpression)) {
                return null;
            }

            current = arrayCreationExpression;
        }
    }

    /**
     * Returns the normalized service id for the surrounding service entry.
     */
    @Nullable
    public static String getServiceId(@NotNull PsiElement element) {
        ArrayHashElement serviceEntry = getServiceEntry(element);
        if (serviceEntry == null) {
            return null;
        }

        String serviceId = getArrayKey(serviceEntry.getKey());
        return StringUtils.isNotBlank(serviceId) ? serviceId : null;
    }

    /**
     * Returns the array expression for the current service definition when present.
     */
    @Nullable
    public static ArrayCreationExpression getServiceDefinition(@NotNull PsiElement element) {
        ArrayHashElement serviceEntry = getServiceEntry(element);
        if (serviceEntry == null || !(serviceEntry.getValue() instanceof ArrayCreationExpression arrayCreationExpression)) {
            return null;
        }

        return arrayCreationExpression;
    }

    /**
     * Resolves the effective class for the current service definition.
     */
    @Nullable
    public static String getCurrentServiceClass(@NotNull PsiElement element) {
        ArrayCreationExpression serviceDefinition = getServiceDefinition(element);
        String serviceId = getServiceId(element);

        if (serviceDefinition == null) {
            return null;
        }

        String className = getPsiValue(PhpElementsUtil.getArrayValue(serviceDefinition, "class"));
        if (StringUtils.isNotBlank(className)) {
            return StringUtils.stripStart(className, "\\");
        }

        Boolean isAbstract = PhpElementsUtil.getArrayValueBool(serviceDefinition, "abstract");
        if (PhpElementsUtil.getArrayValue(serviceDefinition, "alias") == null && !(isAbstract != null && isAbstract) && StringUtils.isNotBlank(serviceId)) {
            return StringUtils.stripStart(serviceId, "\\");
        }

        return null;
    }

    /**
     * Extracts a normalized string value from supported PHP PSI nodes.
     */
    @Nullable
    public static String getPsiValue(@Nullable PsiElement psiElement) {
        if (psiElement instanceof StringLiteralExpression stringLiteralExpression) {
            return ServiceContainerUtil.normalizePhpStringValue(stringLiteralExpression.getContents());
        }

        if (psiElement instanceof ClassConstantReference classConstantReference) {
            return PhpElementsUtil.getClassConstantPhpFqn(classConstantReference);
        }

        return null;
    }

    public record ServiceConfigPath(@NotNull String[] segments) {
        public boolean isDecoratesOrParent() {
            return lengthIs(1) && ("decorates".equals(segment(0)) || "parent".equals(segment(0)));
        }

        public boolean isClass() {
            return lengthIs(1) && "class".equals(segment(0));
        }

        public boolean isArgument() {
            return (lengthAtLeast(2) && "arguments".equals(segment(0)))
                || (lengthAtLeast(4) && "calls".equals(segment(0)) && "1".equals(segment(2)));
        }

        public boolean isTag() {
            return (lengthIs(2) && "tags".equals(segment(0)))
                || (lengthIs(3) && "tags".equals(segment(0)) && "name".equals(segment(2)));
        }

        public boolean isFactoryService() {
            return lengthIs(2) && "factory".equals(segment(0)) && "0".equals(segment(1));
        }

        public boolean isFactoryMethod() {
            return lengthIs(2) && "factory".equals(segment(0)) && "1".equals(segment(1));
        }

        public boolean isCallsMethod() {
            return lengthIs(3) && "calls".equals(segment(0)) && "0".equals(segment(2));
        }

        private boolean lengthIs(int expected) {
            return segments.length == expected;
        }

        private boolean lengthAtLeast(int expected) {
            return segments.length >= expected;
        }

        @Nullable
        private String segment(int index) {
            return index >= 0 && index < segments.length ? segments[index] : null;
        }
    }

    /**
     * Checks whether a hash element is a direct child of the top-level services entry.
     */
    private static boolean isServiceEntry(@NotNull ArrayHashElement serviceEntry) {
        PsiElement parent = getImmediateArrayScope(serviceEntry);
        if (!(parent instanceof ArrayCreationExpression servicesArray)) {
            return false;
        }

        PsiElement serviceParent = getImmediateArrayScope(servicesArray);
        if (!(serviceParent instanceof ArrayHashElement servicesEntry)) {
            return false;
        }

        return servicesEntry.getValue() == servicesArray
            && "services".equals(getArrayKey(servicesEntry.getKey()))
            && isInsideAcceptedPhpConfigArray(servicesEntry);
    }

    /**
     * Verifies that the services entry belongs to an accepted PHP config root.
     *
     * Accepted roots:
     * - return ['services' => [...]]
     * - App::config(['services' => [...]])
     */
    private static boolean isInsideAcceptedPhpConfigArray(@NotNull ArrayHashElement servicesEntry) {
        PsiElement arrayParent = getImmediateArrayScope(servicesEntry);
        if (!(arrayParent instanceof ArrayCreationExpression configArray)) {
            return false;
        }

        PsiElement parent = configArray.getParent();

        // App::config(['services' => [...]])
        if (parent instanceof ParameterList parameterList && parameterList.getParent() instanceof MethodReference methodReference) {
            return isConfigFactoryCall(methodReference);
        }

        // return ['services' => [...]]
        if (parent instanceof PhpReturn phpReturn) {
            return isAllowedPhpReturn(phpReturn);
        }

        return false;
    }

    /**
     * Allows top-level returns and configurator-namespace function returns only.
     */
    private static boolean isAllowedPhpReturn(@NotNull PhpReturn phpReturn) {
        Function function = PsiTreeUtil.getParentOfType(phpReturn, Function.class);
        if (function == null) {
            return true;
        }

        if (PsiTreeUtil.getParentOfType(phpReturn, PhpClass.class) != null) {
            return false;
        }

        PsiElement containingFile = phpReturn.getContainingFile();
        if (!(containingFile instanceof PhpFile phpFile)) {
            return false;
        }

        String namespaceName = PhpNamespaceBraceConverter.getAllNamespaces(phpFile).stream()
            .filter(phpNamespace -> PsiTreeUtil.isAncestor(phpNamespace, phpReturn, false))
            .map(phpNamespace -> StringUtils.stripStart(phpNamespace.getFQN(), "\\"))
            .findFirst()
            .orElse("");

        return namespaceName.isEmpty() || ServiceContainerUtil.CONTAINER_CONFIGURATOR.substring(1).startsWith(namespaceName);
    }

    /**
     * Matches App::config([...]) factory calls used by PHP DIC config files.
     */
    private static boolean isConfigFactoryCall(@NotNull MethodReference methodReference) {
        if (!"config".equals(methodReference.getName())) {
            return false;
        }

        PhpExpression classReference = methodReference.getClassReference();
        if (classReference == null) {
            return false;
        }

        PsiElement resolved = classReference.getReference() != null ? classReference.getReference().resolve() : null;
        if (resolved instanceof PhpClass phpClass) {
            return ServiceContainerUtil.CONTAINER_CONFIG_APP.equals(phpClass.getFQN());
        }

        String text = StringUtils.stripStart(classReference.getText(), "\\");
        return "App".equals(text) || ServiceContainerUtil.CONTAINER_CONFIG_APP.substring(1).equals(text);
    }

    /**
     * Reads and normalizes a PHP array key from string or class constant PSI.
     */
    @Nullable
    private static String getArrayKey(@Nullable PsiElement key) {
        if (key == null) {
            return null;
        }

        return StringUtils.stripStart(getPsiValue(key), "\\");
    }

    /**
     * Returns the path segment for a keyed service attribute entry.
     */
    @NotNull
    private static String getArrayHashSegment(@NotNull ArrayHashElement arrayHashElement) {
        String key = getArrayKey(arrayHashElement.getKey());
        if (key != null) {
            return key;
        }
        return "-1";
    }

    /**
     * Resolves the positional index for a value inside a PHP array literal.
     */
    @NotNull
    private static String getArrayValueIndex(@NotNull PsiElement current, @NotNull ArrayCreationExpression parentArray) {
        PsiElement[] values = PhpElementsUtil.getArrayValues(parentArray);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                return String.valueOf(i);
            }
        }

        return "-1";
    }

    /**
     * Returns the parent array when the element is a direct positional array value.
     */
    @Nullable
    private static ArrayCreationExpression getParentArrayForValue(@NotNull PsiElement element) {
        PsiElement parentScope = getImmediateArrayScope(element);
        if (!(parentScope instanceof ArrayCreationExpression arrayCreationExpression)) {
            return null;
        }

        for (PsiElement value : PhpElementsUtil.getArrayValues(arrayCreationExpression)) {
            if (value == element) {
                return arrayCreationExpression;
            }
        }

        return null;
    }

    /**
     * Returns the direct keyed array entry for the current value node.
     */
    @Nullable
    private static ArrayHashElement getDirectArrayHashElement(@NotNull PsiElement element) {
        PsiElement parentScope = getImmediateArrayScope(element);
        if (!(parentScope instanceof ArrayHashElement arrayHashElement)) {
            return null;
        }

        return arrayHashElement.getValue() != null && PsiTreeUtil.isAncestor(arrayHashElement.getValue(), element, false)
            ? arrayHashElement
            : null;
    }

    /**
     * Steps to the next explicit array scope: keyed entry or positional array.
     */
    @Nullable
    private static PsiElement getImmediateArrayScope(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof ArrayHashElement || parent instanceof ArrayCreationExpression) {
                return parent;
            }

            parent = parent.getParent();
        }

        return null;
    }
}
