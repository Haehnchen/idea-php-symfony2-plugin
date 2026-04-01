package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.DoctrineModelExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataFileStubIndex extends FileBasedIndexExtension<String, DoctrineModelSerializable> {

    public static final ID<String, DoctrineModelSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.doctrine_metadata");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final DoctrineModelExternalizer EXTERNALIZER = DoctrineModelExternalizer.INSTANCE;

    private static final int MAX_FILE_BYTE_SIZE = 1048576;

    private static class MyStringStringFileContentDataIndexer implements DataIndexer<String, DoctrineModelSerializable, FileContent> {
        @NotNull
        @Override
        public Map<String, DoctrineModelSerializable> map(@NotNull FileContent fileContent) {

            Map<String, DoctrineModelSerializable> map = new HashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !StubIndexValidationUtil.isValidForIndex(
                fileContent,
                psiFile,
                MAX_FILE_BYTE_SIZE,
                false,
                Set.of("xml", "yml", "yaml", "php")
            )) {
                return map;
            }

            Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFile);
            if(classRepositoryPair == null || classRepositoryPair.isEmpty()) {
                return map;
            }

            for (DoctrineClassMetadata metadata : classRepositoryPair) {
                String className = metadata.className();
                if(className.isEmpty()) {
                    continue;
                }

                map.put(className, new DoctrineModel(className, metadata.repositoryClass(), metadata.tableName()));
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
        return FileInputFilter.XML_YAML_PHP;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 5;
    }

}
