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
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringListDataExternalizer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generalized index for PHP attributes on classes and methods
 *
 * Maps attribute FQNs to their targets with additional data:
 * - Key: Attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter", "\Symfony\Component\Console\Attribute\AsCommand")
 * - Value: List<String> where:
 *   [0] = Class FQN (e.g., "App\Twig\AppExtension")
 *   [1] = Method name (for method-level attributes) or attribute parameter (e.g., filter name)
 *   [2+] = Additional data (extensible for future use)
 *
 * Examples:
 * - AsTwigFilter: ["App\Twig\AppExtension", "formatProductNumber", "product_number_filter"]
 * - AsCommand: ["App\Command\CreateUserCommand"]
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeIndex extends FileBasedIndexExtension<String, List<String>> {
    public static final ID<String, List<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.php_attribute.index");

    @Override
    public @NotNull ID<String, List<String>> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, List<String>, FileContent> getIndexer() {
        return new PhpAttributeIndexer();
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<List<String>> getValueExternalizer() {
        return StringListDataExternalizer.INSTANCE;
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
        return 6;
    }

    public static class PhpAttributeIndexer implements DataIndexer<String, List<String>, FileContent> {
        // Twig attributes on methods
        private static final Set<String> TWIG_METHOD_ATTRIBUTES = Set.of(
                "\\Twig\\Attribute\\AsTwigFilter",
                "\\Twig\\Attribute\\AsTwigFunction",
                "\\Twig\\Attribute\\AsTwigTest"
        );

        // Symfony console command attributes on classes
        private static final String AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";

        @Override
        public @NotNull Map<String, List<String>> map(@NotNull FileContent inputData) {
            Map<String, List<String>> result = new HashMap<>();
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
        private void processClassAttributes(@NotNull PhpClass phpClass, @NotNull Map<String, List<String>> result) {
            for (PhpAttribute attribute : phpClass.getAttributes()) {
                String attributeFqn = attribute.getFQN();
                if (attributeFqn == null) {
                    continue;
                }

                // Index AsCommand attribute on class
                // Key: Attribute FQN
                // Value: [class FQN]
                if (AS_COMMAND_ATTRIBUTE.equals(attributeFqn)) {
                    String classFqn = StringUtils.stripStart(phpClass.getFQN(), "\\");
                    result.put(attributeFqn, List.of(classFqn));
                }
            }
        }

        /**
         * Process attributes on method level (Twig attributes)
         */
        private void processMethodAttributes(@NotNull PhpClass phpClass, @NotNull Method method, @NotNull Map<String, List<String>> result) {
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
                        result.put(attributeFqn, List.of(classFqn, method.getName(), nameAttribute));
                    }
                }
            }
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
