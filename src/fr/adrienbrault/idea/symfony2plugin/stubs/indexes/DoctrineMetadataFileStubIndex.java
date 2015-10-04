package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DoctrineMetadataFileStubIndex extends FileBasedIndexExtension<String, String> {

    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.doctrine_metadata");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    private static class MyStringStringFileContentDataIndexer implements DataIndexer<String, String, FileContent> {
        @NotNull
        @Override
        public Map<String, String> map(@NotNull FileContent fileContent) {

            Map<String, String> map = new HashMap<String, String>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFile);
            if(classRepositoryPair != null && classRepositoryPair.size() > 0) {
                for (Pair<String, String> pair : classRepositoryPair) {
                    map.put(pair.getFirst(), pair.getSecond());
                }
            }

            return map;
        }
    }

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return new MyStringStringFileContentDataIndexer();
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return ContainerParameterStubIndex.StringDataExternalizer.STRING_DATA_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                FileType fileType = file.getFileType();
                return fileType == XmlFileType.INSTANCE || fileType == YAMLFileType.YML || fileType == PhpFileType.INSTANCE;
            }
        };
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
