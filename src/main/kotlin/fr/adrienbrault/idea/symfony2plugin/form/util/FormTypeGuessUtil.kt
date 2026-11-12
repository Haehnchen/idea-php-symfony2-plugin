package fr.adrienbrault.idea.symfony2plugin.form.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpClassFieldsList
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex
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

/**
 * Returns a form type FQN from Doctrine or PHP, or null.
 * Unknown Doctrine types fall back to TextType when PHP gives no match.
 *
 * ```php
 * public ?int $capacity; // IntegerType
 *
 * #[ORM\Column(type: 'date')]
 * public ?\DateTimeImmutable $day; // DateType
 *
 * #[ORM\ManyToOne(targetEntity: Category::class)]
 * public ?Category $category; // EntityType
 *
 * $builder->add('seats', null, ['property_path' => 'capacity']); // IntegerType
 * ```
 */
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

    val doctrineFields = CachedValuesManager.getCachedValue(dataClass) {
        val metadata = DoctrineMetadataUtil.getModelFields(dataClass.project, dataClass.fqn)
        val fields = metadata?.fields?.associateBy { it.name } ?: emptyMap()

        CachedValueProvider.Result.create(fields, getFormTypeDependencies(dataClass))
    }

    val doctrineField = doctrineFields[propertyName]
    if (doctrineField?.relationType != null) {
        return "\\Symfony\\Bridge\\Doctrine\\Form\\Type\\EntityType"
    }

    val propertyType = dataClass.findFieldByName(propertyName, false)
        ?.declaredType?.filterNull()?.types?.singleOrNull()?.trimStart('\\')?.lowercase(Locale.ROOT)

    // Only native types supported by Symfony participate in guessing.
    // public string $title has no native guess; #[ORM\Column(type: 'string')] gives TextType.
    val nativeType = when (propertyType) {
        "int" -> "integer"
        "bool" -> "boolean"
        "float", "dateinterval" -> propertyType
        "datetime", "datetimeimmutable", "datetimeinterface", "symfony\\component\\clock\\datepoint" -> "datetime"
        else -> null
    }

    // Try the mapped column type first: a PHP DateTimeImmutable may store only a date or time.
    return sequenceOf(doctrineField?.typeName, nativeType).firstNotNullOfOrNull { type ->
        when (type) {
            "integer", "bigint", "smallint" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType"
            "float", "decimal" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\NumberType"
            "boolean" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CheckboxType"
            "string" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType"
            "text" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextareaType"
            "simple_array" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CollectionType"
            "date", "date_immutable" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateType"
            "time", "time_immutable" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TimeType"
            "datetime", "datetime_immutable", "datetimetz", "datetimetz_immutable", "vardatetime" ->
                "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType"
            "dateinterval" -> "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateIntervalType"
            else -> null
        }
    } ?: if (doctrineField != null) "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType" else null
}

/**
 * Tracks PHP declarations and the class's Doctrine mappings for cached form types.
 */
fun getFormTypeDependencies(dataClass: PhpClass): List<Any> {
    val project = dataClass.project
    val mappingFiles = DoctrineMetadataUtil.findMetadataFiles(project, dataClass.fqn)
        .mapNotNull { PsiManager.getInstance(project).findFile(it) }

    return mappingFiles + listOf(
        // PHP constants, parent classes and traits can live outside the mapped file.
        PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE),
        FileIndexCaches.getModificationTrackerForIndexId(project, DoctrineMetadataFileStubIndex.KEY)
    )
}
