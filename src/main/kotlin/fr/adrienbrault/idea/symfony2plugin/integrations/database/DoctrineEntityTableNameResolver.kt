package fr.adrienbrault.idea.symfony2plugin.integrations.database

import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object DoctrineEntityTableNameResolver {

    private val TABLE_NAME_IN_TAG: Pattern = Pattern.compile("name\\s*=\\s*[\"']([^\"']+)[\"']")

    /**
     * Returns `true` if the given `tableName` corresponds to the Doctrine entity represented
     * by `phpClass`.
     *
     * The check is performed in two steps:
     * 1. If the class declares an explicit table name (via XML/YAML mapping, PHP attribute, or
     *    annotation), that name is compared case-insensitively to `tableName`.
     * 2. Otherwise the table name is guessed from the class short name by converting it to
     *    snake_case and optionally pluralizing it (e.g. `FooBar` → `foo_bar` / `foo_bars`),
     *    and the result is compared case-insensitively to `tableName`.
     *
     * @param phpClass  the Doctrine entity class to check
     * @param tableName the database table name to match against (schema-qualified names like
     *                  `public.foo` and quoted identifiers like `` `foo` `` are normalized automatically)
     * @return `true` if the table name matches the entity, `false` otherwise
     */
    @JvmStatic
    fun isTableMatch(phpClass: PhpClass, tableName: String): Boolean {
        val normalizedTableName = normalizeTableName(tableName)

        val explicitTableName = getExplicitTableName(phpClass)
        if (explicitTableName != null) {
            return normalizeTableName(explicitTableName).equals(normalizedTableName, ignoreCase = true)
        }

        return guessTableNames(phpClass).any {
            normalizeTableName(it).equals(normalizedTableName, ignoreCase = true)
        }
    }

    private fun getExplicitTableName(phpClass: PhpClass): String? {
        val model = DoctrineMetadataUtil.getModelFields(phpClass.project, phpClass.presentableFQN)
        if (model != null && StringUtils.isNotBlank(model.table)) {
            return model.table
        }

        return getTableNameFromAttributes(phpClass) ?: getTableNameFromAnnotations(phpClass)
    }

    private fun guessTableNames(phpClass: PhpClass): Set<String> {
        val className = phpClass.name
        if (StringUtils.isBlank(className)) return emptySet()

        val singular = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(className)
        if (StringUtils.isBlank(singular)) return emptySet()

        val names = linkedSetOf(singular)
        val plural = StringUtil.pluralize(singular)
        if (StringUtils.isNotBlank(plural)) {
            names.add(plural)
        }

        return names
    }

    private fun getTableNameFromAttributes(phpClass: PhpClass): String? {
        for (attribute in phpClass.attributes) {
            val fqn = attribute.fqn ?: continue
            if (!PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Table")) continue

            val tableName = PhpElementsUtil.findAttributeArgumentByNameAsString("name", attribute)
            if (StringUtils.isNotBlank(tableName)) {
                return tableName
            }
        }

        return null
    }

    private fun getTableNameFromAnnotations(phpClass: PhpClass): String? {
        val docComment = phpClass.docComment ?: return null

        val tableTag = AnnotationBackportUtil.getReference(docComment, "\\Doctrine\\ORM\\Mapping\\Table")
            ?: return null

        val matcher = TABLE_NAME_IN_TAG.matcher(tableTag.text)
        if (!matcher.find()) return null

        return matcher.group(1)
    }

    private fun normalizeTableName(tableName: String): String = tableName.trim().lowercase()
}
