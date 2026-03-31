package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for detecting and navigating PHP-based Symfony config files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class PhpConfigUtil {

    private PhpConfigUtil() {
    }

    /**
     * Returns true when the string literal is the key of a root-level config entry.
     *
     * Matches: {@code 'framework' => [...]} at the top level of the config array.
     */
    public static boolean isRootConfigKey(@NotNull StringLiteralExpression element) {
        ArrayHashElement hashElement = getParentHashElementAsKey(element);
        if (hashElement == null) {
            return false;
        }

        PsiElement arrayParent = hashElement.getParent();
        if (!(arrayParent instanceof ArrayCreationExpression array)) {
            return false;
        }

        return isAcceptedConfigRootArray(array);
    }

    /**
     * Returns the {@link ArrayHashElement} for which the given string literal is the key.
     */
    @Nullable
    private static ArrayHashElement getParentHashElementAsKey(@NotNull StringLiteralExpression element) {
        ArrayHashElement hashElement = PhpConfigPsiUtil.getParentHashElement(element);
        if (hashElement == null) {
            return null;
        }
        PsiElement key = hashElement.getKey();
        if (key == null) {
            return null;
        }
        if (key == element || PsiTreeUtil.isAncestor(key, element, false)) {
            return hashElement;
        }
        return null;
    }

    /**
     * Returns true when the string literal is the key of a config entry nested directly
     * inside a root-level {@code when@...} block.
     *
     * Matches: {@code 'framework'} in {@code 'when@prod' => ['framework' => [...]]}
     */
    public static boolean isConditionalConfigKey(@NotNull StringLiteralExpression element) {
        ArrayHashElement innerHash = getParentHashElementAsKey(element);
        if (innerHash == null) {
            return false;
        }

        // parent array of the inner hash
        PsiElement innerArray = innerHash.getParent();
        if (!(innerArray instanceof ArrayCreationExpression)) {
            return false;
        }

        // that array must be the value of a when@... root key (may be wrapped in a PhpPsiElement)
        PsiElement whenHashParent = innerArray.getParent();
        if (!(whenHashParent instanceof ArrayHashElement)) {
            whenHashParent = whenHashParent != null ? whenHashParent.getParent() : null;
        }
        if (!(whenHashParent instanceof ArrayHashElement whenHashElement)) {
            return false;
        }

        PsiElement whenHashValue = whenHashElement.getValue();
        if (whenHashValue == null || whenHashValue.getTextOffset() != innerArray.getTextOffset()) {
            return false;
        }

        String whenKeyText = getHashKeyContents(whenHashElement);
        if (whenKeyText == null || !whenKeyText.startsWith("when@")) {
            return false;
        }

        // the when@ hash must be at the root config array level
        ArrayCreationExpression rootArray = PhpConfigPsiUtil.getParentArray(whenHashElement);
        if (rootArray == null) {
            return false;
        }

        return isAcceptedConfigRootArray(rootArray);
    }

    /**
     * Returns true when the string literal is a {@code resource} value inside an
     * {@code imports} block at the root level or inside a root-level {@code when@...} block.
     *
     * Matches:
     * {@code 'imports' => [['resource' => 'legacy_config.php']]}
     * {@code 'when@prod' => ['imports' => [['resource' => 'legacy_config.php']]]}
     */
    public static boolean isImportResourceValue(@NotNull StringLiteralExpression element) {
        // element must be the value of a 'resource' key
        ArrayHashElement resourceHash = getParentHashElementAsValue(element);
        if (resourceHash == null) {
            return false;
        }

        if (!"resource".equals(getHashKeyContents(resourceHash))) {
            return false;
        }

        // the resource hash must be inside a positional import entry array
        PsiElement importEntryArray = resourceHash.getParent();
        if (!(importEntryArray instanceof ArrayCreationExpression)) {
            return false;
        }

        ArrayCreationExpression importsArray;
        ArrayCreationExpression directParentArray = PhpConfigPsiUtil.getParentArray((ArrayCreationExpression) importEntryArray);
        if (directParentArray != null) {
            importsArray = directParentArray;
        } else {
            ArrayHashElement importEntryHash = getParentHashElementAsValue((ArrayCreationExpression) importEntryArray);
            if (importEntryHash == null) {
                return false;
            }

            if (importEntryHash.getKey() instanceof StringLiteralExpression) {
                return false;
            }

            if (!(importEntryHash.getParent() instanceof ArrayCreationExpression arrayCreationExpression)) {
                return false;
            }

            importsArray = arrayCreationExpression;
        }

        // importsArray must be the value of an 'imports' key
        ArrayHashElement importsHash = getParentHashElementAsValue(importsArray);
        if (importsHash == null) {
            return false;
        }

        if (!"imports".equals(getHashKeyContents(importsHash))) {
            return false;
        }

        // the imports hash must be at root level or inside a when@ block
        ArrayCreationExpression importsParentArrayExpr = PhpConfigPsiUtil.getParentArray(importsHash);
        if (importsParentArrayExpr == null) {
            return false;
        }

        if (isAcceptedConfigRootArray(importsParentArrayExpr)) {
            return true;
        }

        // check when@ nesting: importsParentArray is the value of a when@... root key
        ArrayHashElement whenHashElement = getParentHashElementAsValue(importsParentArrayExpr);
        if (whenHashElement == null) {
            return false;
        }

        String whenKeyText = getHashKeyContents(whenHashElement);
        if (whenKeyText == null || !whenKeyText.startsWith("when@")) {
            return false;
        }

        ArrayCreationExpression rootArray = PhpConfigPsiUtil.getParentArray(whenHashElement);
        return rootArray != null && isAcceptedConfigRootArray(rootArray);
    }

    /**
     * Returns true when the given {@link ArrayCreationExpression} is the top-level config array
     * of an accepted PHP config root, determined purely by parent-scope inspection:
     * - {@code return [ ... ];}
     * - {@code return App::config([ ... ]);}
     * - {@code App::config([ ... ]);} (statement, no return)
     */
    public static boolean isAcceptedConfigRootArray(@NotNull ArrayCreationExpression array) {
        PsiElement parent = array.getParent();

        // return [ ... ];
        if (parent instanceof PhpReturn phpReturn) {
            return isAllowedPhpReturn(phpReturn);
        }

        // return App::config([ ... ]); or App::config([ ... ]); as statement
        if (parent instanceof ParameterList parameterList && parameterList.getParent() instanceof MethodReference methodReference) {
            if (!PhpConfigPsiUtil.isConfigFactoryCall(methodReference)) {
                return false;
            }
            PsiElement methodParent = methodReference.getParent();
            if (methodParent instanceof PhpReturn phpReturn) {
                return isAllowedPhpReturn(phpReturn);
            }
            // statement form: App::config([...]) not inside a return
            return true;
        }

        return false;
    }

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
     * Returns the {@link ArrayHashElement} for which the given element is the value.
     */
    @Nullable
    private static ArrayHashElement getParentHashElementAsValue(@NotNull PsiElement element) {
        ArrayHashElement hashElement = PhpConfigPsiUtil.getParentHashElement(element);
        if (hashElement == null) {
            return null;
        }
        PsiElement value = hashElement.getValue();
        if (value == null) {
            return null;
        }
        if (value == element || PsiTreeUtil.isAncestor(value, element, false)) {
            return hashElement;
        }
        return null;
    }

    /**
     * Returns the string contents of the key of an {@link ArrayHashElement}.
     */
    @Nullable
    private static String getHashKeyContents(@NotNull ArrayHashElement hashElement) {
        return hashElement.getKey() instanceof StringLiteralExpression str ? str.getContents() : null;
    }

}
