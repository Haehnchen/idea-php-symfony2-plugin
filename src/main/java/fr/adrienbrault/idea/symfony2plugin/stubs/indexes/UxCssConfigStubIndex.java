package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collections;
import java.util.Map;

/**
 * The YAML and PHP files that configure symfony/ux-css, under the single key {@code ux_css}.
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class UxCssConfigStubIndex extends FileBasedIndexExtension<String, Void> {
    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.ux_css_config");
    private static final int MAX_FILE_BYTE_SIZE = 2097152;
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            if (!Symfony2ProjectComponent.isEnabledForIndex(inputData.getProject())
                || inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE
                || !inputData.getContentAsText().toString().contains(UxCssConfigUtil.ROOT_KEY)
            ) {
                return Collections.emptyMap();
            }

            PsiFile psiFile = inputData.getPsiFile();
            if (!StubIndexValidationUtil.isValidForIndex(inputData, psiFile, MAX_FILE_BYTE_SIZE, true, null)) {
                return Collections.emptyMap();
            }

            boolean isConfig = psiFile instanceof YAMLFile yamlFile
                ? !UxCssConfigUtil.getYamlConfigs(yamlFile).isEmpty()
                : psiFile instanceof PhpFile phpFile && !UxCssConfigUtil.getPhpConfigs(phpFile).isEmpty();

            return isConfig ? Collections.singletonMap(UxCssConfigUtil.ROOT_KEY, null) : Collections.emptyMap();
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == YAMLFileType.YML || file.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
