package fr.adrienbrault.idea.symfony2plugin.form.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpClassFieldsList
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import java.util.Locale

/**
 * Returns the explicit form type or guesses it from the property type.
 *
 * ```php
 * #[FormField]
 * public ?int $capacity; // IntegerType
 * ```
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
fun getFormType(attribute: PhpAttribute): String? {
    val type = PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "type")
    if (type != null && !PhpLangUtil.isNull(type)) {
        val explicitType = PhpElementsUtil.getStringValue(type)

        return if (explicitType != null) "\\" + explicitType.trimStart('\\') else FormUtil.getFormTypeFqnOnParameter(type)
    }

    // Find the attributed property, including promoted constructor properties.
    // #[FormField] public int $capacity;
    // public function __construct(#[FormField] public int $capacity) {}
    val field = when (val declaration = attribute.parent.parent) {
        is Field -> declaration
        is PhpClassFieldsList -> PsiTreeUtil.getChildOfType(declaration, Field::class.java)
        else -> null
    } ?: return null
    val dataClass = field.containingClass ?: return null

    return guessType(
        dataClass, field.name,
        PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 1, "options")
    )
}

/**
 * Returns the explicit form type or guesses it from the form's data class property.
 *
 * ```php
 * // The data class has: public ?int $capacity;
 * $builder->add('capacity'); // IntegerType
 * ```
 */
fun getFormType(method: MethodReference): String? {
    if (!PhpElementsUtil.isMethodReferenceInstanceOf(method, *FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
        return null
    }

    val parameters = method.parameters
    val type = PsiElementUtils.getParameterOrNamedArgument(parameters, "type", 1)
    if (type != null && !PhpLangUtil.isNull(type)) {
        return FormUtil.getFormTypeFqnOnParameter(type)
    }

    // Find the field name and data class to infer the property type.
    // $builder->add('capacity'); // field: capacity
    // $resolver->setDefaults(['data_class' => EventData::class]); // data class: EventData
    val fieldName = PhpElementsUtil.getStringValue(
        PsiElementUtils.getParameterOrNamedArgument(parameters, if (method.name == "create") "name" else "child", 0)
    ) ?: return null
    val dataClass = FormOptionsUtil.getFormPhpClassFromContext(method) ?: return null

    return guessType(
        dataClass, fieldName,
        PsiElementUtils.getParameterOrNamedArgument(parameters, "options", 2)
    )
}

private fun guessType(dataClass: PhpClass, fieldName: String, options: PsiElement?): String? {
    var propertyName = fieldName
    if (options is ArrayCreationExpression) {
        val path = PhpElementsUtil.getStringValue(PhpElementsUtil.getArrayValue(options, "property_path"))

        // Use simple property names such as "capacity".
        // Paths such as "address.city" or "items[0]" fall back to the field name.
        if (!path.isNullOrEmpty() && path.none { it in ".[]?\\" }) {
            propertyName = path
        }
    }

    val field = dataClass.findFieldByName(propertyName, false) ?: return null
    val type = field.declaredType.filterNull().types.singleOrNull() ?: return null

    // Symfony creates DateTimeImmutable itself, so a property requiring a subclass would reject the value.
    return when (type.trimStart('\\').lowercase(Locale.ROOT)) {
        "int" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType"
        "float" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\NumberType"
        "bool" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CheckboxType"
        "datetime", "datetimeimmutable", "datetimeinterface", "symfony\\component\\clock\\datepoint" ->
            "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType"
        "dateinterval" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateIntervalType"
        else -> null
    }
}
