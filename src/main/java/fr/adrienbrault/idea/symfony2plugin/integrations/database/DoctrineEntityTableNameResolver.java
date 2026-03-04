package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoctrineEntityTableNameResolver {
    private static final Pattern TABLE_NAME_IN_TAG = Pattern.compile("name\\s*=\\s*[\"']([^\"']+)[\"']");

    private DoctrineEntityTableNameResolver() {
    }

    /**
     * Returns {@code true} if the given {@code tableName} corresponds to the Doctrine entity represented
     * by {@code phpClass}.
     *
     * <p>The check is performed in two steps:
     * <ol>
     *   <li>If the class declares an explicit table name (via XML/YAML mapping, PHP attribute, or
     *       annotation), that name is compared case-insensitively to {@code tableName}.</li>
     *   <li>Otherwise the table name is guessed from the class short name by converting it to
     *       snake_case and optionally pluralizing it (e.g. {@code FooBar} → {@code foo_bar} /
     *       {@code foo_bars}), and the result is compared case-insensitively to {@code tableName}.</li>
     * </ol>
     *
     * @param phpClass  the Doctrine entity class to check
     * @param tableName the database table name to match against (schema-qualified names like
     *                  {@code public.foo} and quoted identifiers like {@code `foo`} are normalized
     *                  automatically)
     * @return {@code true} if the table name matches the entity, {@code false} otherwise
     */
    public static boolean isTableMatch(@NotNull PhpClass phpClass, @NotNull String tableName) {
        String normalizedTableName = normalizeTableName(tableName);

        String explicitTableName = getExplicitTableName(phpClass);
        if (explicitTableName != null) {
            return normalizeTableName(explicitTableName).equalsIgnoreCase(normalizedTableName);
        }

        for (String guessedTableName : guessTableNames(phpClass)) {
            if (normalizeTableName(guessedTableName).equalsIgnoreCase(normalizedTableName)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static String getExplicitTableName(@NotNull PhpClass phpClass) {
        DoctrineMetadataModel model = DoctrineMetadataUtil.getModelFields(phpClass.getProject(), phpClass.getPresentableFQN());
        if (model != null && StringUtils.isNotBlank(model.getTable())) {
            return model.getTable();
        }

        String tableFromAttributes = getTableNameFromAttributes(phpClass);
        if (tableFromAttributes != null) {
            return tableFromAttributes;
        }

        return getTableNameFromAnnotations(phpClass);
    }

    @NotNull
    private static Set<String> guessTableNames(@NotNull PhpClass phpClass) {
        Set<String> names = new LinkedHashSet<>();

        String className = phpClass.getName();
        if (StringUtils.isBlank(className)) {
            return names;
        }

        String singular = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(className);
        if (StringUtils.isNotBlank(singular)) {
            names.add(singular);

            String plural = StringUtil.pluralize(singular);
            if (StringUtils.isNotBlank(plural)) {
                names.add(plural);
            }
        }

        return names;
    }

    @Nullable
    private static String getTableNameFromAttributes(@NotNull PhpClass phpClass) {
        for (PhpAttribute attribute : phpClass.getAttributes()) {
            String fqn = attribute.getFQN();
            if (fqn == null || !PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Table")) {
                continue;
            }

            String tableName = PhpElementsUtil.findAttributeArgumentByNameAsString("name", attribute);
            if (StringUtils.isNotBlank(tableName)) {
                return tableName;
            }
        }

        return null;
    }

    @Nullable
    private static String getTableNameFromAnnotations(@NotNull PhpClass phpClass) {
        PhpDocComment docComment = phpClass.getDocComment();
        if (docComment == null) {
            return null;
        }

        PhpDocTag tableTag = AnnotationBackportUtil.getReference(docComment, "\\Doctrine\\ORM\\Mapping\\Table");

        if (tableTag == null) {
            return null;
        }

        Matcher matcher = TABLE_NAME_IN_TAG.matcher(tableTag.getText());
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    @NotNull
    private static String normalizeTableName(@NotNull String tableName) {
        return tableName.trim().toLowerCase();
    }
}
