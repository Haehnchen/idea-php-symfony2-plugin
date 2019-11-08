package fr.adrienbrault.idea.symfonyplugin.stubs.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.stubs.dict.TwigMacroTagIndex;
import fr.adrienbrault.idea.symfonyplugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMacroFunctionStubIndex extends FileBasedIndexExtension<String, TwigMacroTagIndex> {

    public static final ID<String, TwigMacroTagIndex> KEY = ID.create("fr.adrienbrault.idea.symfonyplugin.twig_macro_function");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static ObjectStreamDataExternalizer<TwigMacroTagIndex> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, TwigMacroTagIndex> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, TwigMacroTagIndex, FileContent> getIndexer() {
        return inputData -> {
            final Map<String, TwigMacroTagIndex> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!(psiFile instanceof TwigFile)) {
                return map;
            }

            TwigUtil.visitMacros(psiFile, pair -> map.put(pair.getFirst().getName(), new TwigMacroTagIndex(pair.getFirst().getName(), pair.getFirst().getParameters())));

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
    public DataExternalizer<TwigMacroTagIndex> getValueExternalizer() {
        return EXTERNALIZER;
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
        return 3;
    }
}



