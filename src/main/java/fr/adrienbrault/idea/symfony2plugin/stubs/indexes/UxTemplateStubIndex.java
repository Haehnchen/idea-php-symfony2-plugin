package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxTemplateStubIndex extends FileBasedIndexExtension<String, UxComponent> {
    public static final ID<String, UxComponent> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.ux_template_index");
    private static final ObjectStreamDataExternalizer<UxComponent> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    @Override
    public @NotNull ID<String, UxComponent> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, UxComponent, FileContent> getIndexer() {
        return inputData -> {
            Map<String, UxComponent> map = new HashMap<>();

            if(inputData.getPsiFile() instanceof PhpFile phpFile) {
                UxUtil.visitComponentsForIndex(phpFile, t -> map.put(t.phpClass().getFQN(), new UxComponent(t.name(), t.phpClass().getFQN(), t.template(), t.type())));
            }

            return map;
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public @NotNull DataExternalizer<UxComponent> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 3;
    }

    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
