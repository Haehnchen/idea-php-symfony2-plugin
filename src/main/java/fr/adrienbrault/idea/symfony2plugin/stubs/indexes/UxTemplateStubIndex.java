package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxTemplateStubIndex extends FileBasedIndexExtension<String, String> {
    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.ux_template_index");

    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    @Override
    public @NotNull ID<String, String> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            Map<String, String> map = new HashMap<>();

            if(inputData.getPsiFile() instanceof PhpFile phpFile) {
                UxUtil.visitAsTwigComponent(phpFile, pair -> map.put(pair.getFirst(), pair.getSecond().getFQN()));
            }

            return map;
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public @NotNull DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
