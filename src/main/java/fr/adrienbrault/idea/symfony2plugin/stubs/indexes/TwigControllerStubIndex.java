package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * {{ controller("foo::action") }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigControllerStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_controller_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !(psiFile instanceof TwigFile)) {
                return Collections.emptyMap();
            }

            final Map<String, Void> map = new THashMap<>();
            TwigUtil.visitControllerFunctions(psiFile, pair -> {
                String name = pair.getFirst();
                if (name.length() < 255) {
                    map.put(name, null);
                }
            });
            return map;
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
        return file -> file.getFileType() == TwigFileType.INSTANCE;
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
