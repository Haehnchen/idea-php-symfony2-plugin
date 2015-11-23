package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class FileResourcesIndex extends FileBasedIndexExtension<String, FileResource> {

    private static int MAX_FILE_BYTE_SIZE = 1048576;
    private static JsonDataExternalizer JSON_EXTERNALIZER = new JsonDataExternalizer();

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
        return new DataIndexer<String, FileResource, FileContent>() {
            @NotNull
            @Override
            public Map<String, FileResource> map(@NotNull FileContent inputData) {
                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                    return Collections.emptyMap();
                }

                final Map<String, FileResource> items = new THashMap<String, FileResource>();

                FileResourceVisitorUtil.visitFile(psiFile, new Consumer<FileResourceVisitorUtil.FileResourceConsumer>() {
                    @Override
                    public void consume(FileResourceVisitorUtil.FileResourceConsumer consumer) {
                        items.put(consumer.getResource(), consumer.createFileResource());
                    }
                });

                return items;
            }
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
        return JSON_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile virtualFile) {
                return virtualFile.getFileType() == XmlFileType.INSTANCE || virtualFile.getFileType() == YAMLFileType.YML;
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

        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml")|| extension.equalsIgnoreCase("yaml"))) {
            return false;
        }

        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }

    private static class JsonDataExternalizer implements DataExternalizer<FileResource> {

        private static final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();
        private static final Gson GSON = new Gson();

        @Override
        public void save(@NotNull DataOutput dataOutput, FileResource fileResource) throws IOException {
            myStringEnumerator.save(dataOutput, GSON.toJson(fileResource));
        }

        @Override
        public FileResource read(@NotNull DataInput in) throws IOException {
            try {
                return GSON.fromJson(myStringEnumerator.read(in), FileResource.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }
    }
}



