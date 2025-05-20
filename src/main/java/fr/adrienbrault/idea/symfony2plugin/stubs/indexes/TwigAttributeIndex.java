package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAttributeIndex extends FileBasedIndexExtension<String, String> {
    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_attribute_extension.index");

    @Override
    public @NotNull ID<String, String> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, String, FileContent> getIndexer() {
        return new TwigAttributeIndexer();
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
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
        return 1;
    }

    public static class TwigAttributeIndexer implements DataIndexer<String, String, FileContent> {
        private static final String[] SUPPORTED_ATTRIBUTES = {
                "\\Twig\\Attribute\\AsTwigFilter",
                "\\Twig\\Attribute\\AsTwigFunction",
                "\\Twig\\Attribute\\AsTwigTest",
        };

        @Override
        public @NotNull Map<String, String> map(@NotNull FileContent inputData) {
            Map<String, String> result = new HashMap<>();
            if (!(inputData.getPsiFile() instanceof PhpFile phpFile)) {
                return result;
            }

            for (PhpClass phpClass : PhpPsiUtil.findAllClasses(phpFile)) {
                for (Method method : phpClass.getOwnMethods()) {
                    processMethodAttributes(phpClass, method, result);
                }
            }

            return result;
        }

        private void processMethodAttributes(@NotNull PhpClass phpClass, @NotNull Method method, @NotNull Map<String, String> result) {
            for (PhpAttribute attribute : method.getAttributes()) {
                String attributeFqn = attribute.getFQN();
                if (attributeFqn == null) {
                    continue;
                }

                for (String supportedAttr : SUPPORTED_ATTRIBUTES) {
                    if (supportedAttr.equals(attributeFqn)) {
                        String nameAttribute = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
                        if (nameAttribute != null) {
                            result.put(nameAttribute, String.format("#M#C\\%s.%s", StringUtils.stripStart(phpClass.getFQN(), "\\"), method.getName()));
                        }

                        break;
                    }
                }
            }
        }
    }
}