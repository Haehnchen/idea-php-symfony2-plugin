package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourcesIndex extends FileBasedIndexExtension<String, FileResource> {

    private static int MAX_FILE_BYTE_SIZE = 1048576;
    private static ObjectStreamDataExternalizer<FileResource> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    public static final ID<String, FileResource> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.file_resources");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, FileResource> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, FileResource, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                return Collections.emptyMap();
            }

            final Map<String, FileResource> items = new THashMap<>();

            FileResourceVisitorUtil.visitFile(psiFile, consumer ->
                items.put(consumer.getResource(), consumer.createFileResource())
            );

            return items;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<FileResource> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile ->
            virtualFile.getFileType() == XmlFileType.INSTANCE || virtualFile.getFileType() == YAMLFileType.YML;
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

        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml")|| extension.equalsIgnoreCase("yaml"))) {
            return false;
        }

        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }
}



