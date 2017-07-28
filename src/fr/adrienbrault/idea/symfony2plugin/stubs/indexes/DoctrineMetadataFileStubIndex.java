package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataFileStubIndex extends FileBasedIndexExtension<String, DoctrineModelSerializable> {

    public static final ID<String, DoctrineModelSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.doctrine_metadata");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static ObjectStreamDataExternalizer<DoctrineModelSerializable> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    private static int MAX_FILE_BYTE_SIZE = 1048576;

    private static class MyStringStringFileContentDataIndexer implements DataIndexer<String, DoctrineModelSerializable, FileContent> {
        @NotNull
        @Override
        public Map<String, DoctrineModelSerializable> map(@NotNull FileContent fileContent) {

            Map<String, DoctrineModelSerializable> map = new THashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(fileContent, psiFile)) {
                return map;
            }

            Collection<Pair<String, String>> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFile);
            if(classRepositoryPair == null || classRepositoryPair.size() == 0) {
                return map;
            }

            for (Pair<String, String> pair : classRepositoryPair) {
                String first = pair.getFirst();
                if(first == null || first.length() == 0) {
                    continue;
                }

                map.put(first, new DoctrineModel(first, pair.getSecond()));
            }

            return map;
        }
    }

    @NotNull
    @Override
    public ID<String, DoctrineModelSerializable> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, DoctrineModelSerializable, FileContent> getIndexer() {
        return new MyStringStringFileContentDataIndexer();
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<DoctrineModelSerializable> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> {
            FileType fileType = virtualFile.getFileType();
            return
                fileType == XmlFileType.INSTANCE ||
                fileType == PhpFileType.INSTANCE ||
                fileType == YAMLFileType.YML
            ;
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();

        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        // @TODO: filter .orm.xml?
        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml")|| extension.equalsIgnoreCase("yaml") || extension.equalsIgnoreCase("php"))) {
            return false;
        }

        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }
}
