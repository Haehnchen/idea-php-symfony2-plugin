package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class DoctrineMetadataFileStubIndex extends FileBasedIndexExtension<String, DoctrineModelInterface> {

    public static final ID<String, DoctrineModelInterface> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.doctrine_metadata_json");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static JsonDataExternalizer JSON_EXTERNALIZER = new JsonDataExternalizer();

    private static int MAX_FILE_BYTE_SIZE = 1048576;

    private static class MyStringStringFileContentDataIndexer implements DataIndexer<String, DoctrineModelInterface, FileContent> {
        @NotNull
        @Override
        public Map<String, DoctrineModelInterface> map(@NotNull FileContent fileContent) {

            Map<String, DoctrineModelInterface> map = new THashMap<String, DoctrineModelInterface>();

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
                map.put(first, new DoctrineModel(first).setRepositoryClass(pair.getSecond()));
            }

            return map;
        }
    }

    @NotNull
    @Override
    public ID<String, DoctrineModelInterface> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, DoctrineModelInterface, FileContent> getIndexer() {
        return new MyStringStringFileContentDataIndexer();
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<DoctrineModelInterface> getValueExternalizer() {
        return JSON_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile virtualFile) {
                FileType fileType = virtualFile.getFileType();
                return
                    fileType == XmlFileType.INSTANCE ||
                    fileType == PhpFileType.INSTANCE ||
                    fileType == YAMLFileType.YML
                ;
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

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();

        if(fileName.startsWith(".") || fileName.contains("Test")) {
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

    private static class JsonDataExternalizer implements DataExternalizer<DoctrineModelInterface> {

        private static final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();
        private static final Gson GSON = new Gson();

        @Override
        public void save(@NotNull DataOutput dataOutput, DoctrineModelInterface fileResource) throws IOException {
            myStringEnumerator.save(dataOutput, GSON.toJson(fileResource));
        }

        @Override
        public DoctrineModelInterface read(@NotNull DataInput in) throws IOException {
            try {
                return GSON.fromJson(myStringEnumerator.read(in), DoctrineModel.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }
    }
}
