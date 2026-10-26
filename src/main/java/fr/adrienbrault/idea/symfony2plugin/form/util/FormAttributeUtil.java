package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE;
import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class FormAttributeUtil {
    public static final String DEFAULT_FORM_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType";

    /**
     * Returns the options declared in this class's AsFormType attribute.
     */
    @Nullable
    public static ArrayCreationExpression getDefaultOptions(@NotNull PhpClass phpClass) {
        for (PhpAttribute attribute : phpClass.getAttributes(AS_FORM_TYPE_ATTRIBUTE)) {
            return PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "options") instanceof ArrayCreationExpression options
                ? options : null;
        }

        return null;
    }

    /**
     * Finds the nearest superclass with its own AsFormType attribute, skipping plain classes.
     */
    @Nullable
    public static PhpClass getParentFormType(@NotNull PhpClass phpClass) {
        Set<String> visited = new HashSet<>();
        for (PhpClass parent = phpClass.getSuperClass(); parent != null && visited.add(parent.getFQN()); parent = parent.getSuperClass()) {
            if (!parent.getAttributes(AS_FORM_TYPE_ATTRIBUTE).isEmpty()) {
                return parent;
            }
        }

        return null;
    }

    /**
     * Returns the form attribute whose options argument is this array.
     */
    @Nullable
    public static PhpAttribute getOptionsAttribute(@NotNull ArrayCreationExpression options) {
        if (!(options.getParent() instanceof ParameterList parameters) || !(parameters.getParent() instanceof PhpAttribute attribute)) {
            return null;
        }

        String fqn = attribute.getFQN();
        int position;
        if (AS_FORM_TYPE_ATTRIBUTE.equals(fqn) && attribute.getParent().getParent() instanceof PhpClass) {
            position = 0;
        } else if (FORM_FIELD_ATTRIBUTE.equals(fqn)) {
            position = 1;
        } else {
            return null;
        }

        return PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, position, "options") == options ? attribute : null;
    }

    /**
     * Resolves a default option on this form type or its attributed parents.
     */
    @Nullable
    public static PhpPsiElement getDefaultOptionValue(@NotNull PhpClass phpClass, @NotNull String option) {
        Set<String> visited = new HashSet<>();
        for (PhpClass current = phpClass; current != null && visited.add(current.getFQN()); current = getParentFormType(current)) {
            ArrayCreationExpression defaults = getDefaultOptions(current);
            if (defaults != null) {
                PhpPsiElement value = PhpElementsUtil.getArrayValue(defaults, option);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }
}
