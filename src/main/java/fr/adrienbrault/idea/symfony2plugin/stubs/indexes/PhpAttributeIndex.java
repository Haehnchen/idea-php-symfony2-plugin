package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.PhpAttributeTargetsDataExternalizer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Generalized index for PHP attributes on classes and methods
 *
 * Maps attribute FQNs to scoped targets with additional data:
 * - Key: Attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter", "\Symfony\Component\Console\Attribute\AsCommand")
 * - Value: scoped targets for supported class and method attributes
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeIndex extends FileBasedIndexExtension<String, List<PhpAttributeIndex.AttributeTarget>> {
    public static final ID<String, List<AttributeTarget>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.php_attribute.index");

    public enum TargetScope {
        PHP_CLASS,
        METHOD
    }

    public record AttributeTarget(
        @NotNull TargetScope scope,
        @NotNull String classFqn,
        @Nullable String memberName,
        @NotNull List<String> data
    ) {
    }

    @Override
    public @NotNull ID<String, List<AttributeTarget>> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, List<AttributeTarget>, FileContent> getIndexer() {
        return new PhpAttributeIndexer();
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<List<AttributeTarget>> getValueExternalizer() {
        return PhpAttributeTargetsDataExternalizer.INSTANCE;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 8;
    }

    public static class PhpAttributeIndexer implements DataIndexer<String, List<AttributeTarget>, FileContent> {
        // Twig attributes on methods
        private static final Set<String> TWIG_METHOD_ATTRIBUTES = Set.of(
                "\\Twig\\Attribute\\AsTwigFilter",
                "\\Twig\\Attribute\\AsTwigFunction",
                "\\Twig\\Attribute\\AsTwigTest"
        );

        // Symfony console command attributes on classes
        private static final String AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";

        // Symfony dependency injection attributes on classes
        public static final String EXCLUDE_ATTRIBUTE = "\\Symfony\\Component\\DependencyInjection\\Attribute\\Exclude";

        @Override
        public @NotNull Map<String, List<AttributeTarget>> map(@NotNull FileContent inputData) {
            Map<String, List<AttributeTarget>> result = new HashMap<>();
            if (!(inputData.getPsiFile() instanceof PhpFile phpFile)) {
                return result;
            }

            for (PhpClass phpClass : PhpPsiUtil.findAllClasses(phpFile)) {
                // Process class-level attributes
                processClassAttributes(phpClass, result);

                // Process method-level attributes
                for (Method method : phpClass.getOwnMethods()) {
                    processMethodAttributes(phpClass, method, result);
                }
            }

            return result;
        }

        /**
         * Process attributes on class level (e.g., AsCommand on Command classes)
         */
        private void processClassAttributes(@NotNull PhpClass phpClass, @NotNull Map<String, List<AttributeTarget>> result) {
            for (PhpAttribute attribute : phpClass.getAttributes()) {
                String attributeFqn = attribute.getFQN();
                if (attributeFqn == null) {
                    continue;
                }

                if (AS_COMMAND_ATTRIBUTE.equals(attributeFqn) || EXCLUDE_ATTRIBUTE.equals(attributeFqn)) {
                    String classFqn = StringUtils.stripStart(phpClass.getFQN(), "\\");
                    addTarget(result, attributeFqn, new AttributeTarget(TargetScope.PHP_CLASS, classFqn, null, List.of()));
                }
            }
        }

        /**
         * Process attributes on method level.
         */
        private void processMethodAttributes(@NotNull PhpClass phpClass, @NotNull Method method, @NotNull Map<String, List<AttributeTarget>> result) {
            for (PhpAttribute attribute : method.getAttributes()) {
                String attributeFqn = attribute.getFQN();
                if (attributeFqn == null) {
                    continue;
                }

                // Index Twig attributes on methods
                // Key: Attribute FQN
                // Value: [class FQN, method name, filter/function/test name]
                if (TWIG_METHOD_ATTRIBUTES.contains(attributeFqn)) {
                    String nameAttribute = extractFirstAttributeParameter(attribute);
                    if (nameAttribute != null) {
                        String classFqn = StringUtils.stripStart(phpClass.getFQN(), "\\");
                        addTarget(result, attributeFqn, new AttributeTarget(TargetScope.METHOD, classFqn, method.getName(), List.of(nameAttribute)));
                    }
                }

                if (AS_COMMAND_ATTRIBUTE.equals(attributeFqn) && method.getAccess().isPublic()) {
                    String classFqn = StringUtils.stripStart(phpClass.getFQN(), "\\");
                    addTarget(result, attributeFqn, new AttributeTarget(TargetScope.METHOD, classFqn, method.getName(), List.of()));
                }
            }
        }

        private void addTarget(
            @NotNull Map<String, List<AttributeTarget>> result,
            @NotNull String attributeFqn,
            @NotNull AttributeTarget target
        ) {
            result.computeIfAbsent(attributeFqn, ignored -> new ArrayList<>()).add(target);
        }

        /**
         * Extract the first parameter from a PHP attribute during indexing
         * We can't use PhpPsiAttributesUtil because it doesn't work reliably during indexing
         */
        private String extractFirstAttributeParameter(@NotNull PhpAttribute attribute) {
            for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                PhpExpectedFunctionArgument funcArg = argument.getArgument();
                if (funcArg != null && funcArg.getArgumentIndex() == 0) {
                    // Try to get the value as a string
                    String value = funcArg.getValue();
                    if (value != null) {
                        // Remove quotes if present
                        return value.replaceAll("^['\"]|['\"]$", "");
                    }
                }
            }
            return null;
        }
    }
}
