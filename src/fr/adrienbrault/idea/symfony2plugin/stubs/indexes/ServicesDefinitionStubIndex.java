package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ArrayDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;
import java.util.List;
import java.util.Map;


public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, SerializableService> {

    private static int MAX_FILE_BYTE_SIZE = 5242880;

    public static final ID<String, SerializableService> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition_object");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static ObjectStreamDataExternalizer<SerializableService> JSON_EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public DataIndexer<String, SerializableService, FileContent> getIndexer() {

        return inputData -> {

            Map<String, SerializableService> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                return map;
            }

            for (SerializableService service : ServiceContainerUtil.getServicesInFile(psiFile)) {
                map.put(service.getId(), service);
            }

            return map;
        };
    }

    @NotNull
    @Override
    public ID<String, SerializableService> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<SerializableService> getValueExternalizer() {
        return JSON_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file ->
            file.getFileType() == XmlFileType.INSTANCE || file.getFileType() == YAMLFileType.YML;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * @deprecated
     */
    public static class MySetDataExternalizer extends ArrayDataExternalizer {};

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.contains("Test")) {
            return false;
        }

        // container file need to be xml file, eg xsd filetypes are not valid
        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml"))) {
            return false;
        }

        // possible fixture or test file
        // to support also library paths, only filter them on project files
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), psiFile.getProject().getBaseDir(), '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Fixture/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        // dont add configured service paths
        List<File> settingsServiceFiles = psiFile.getProject().getComponent(Symfony2ProjectComponent.class).getContainerFiles();
        for(File file: settingsServiceFiles) {
            if(VfsUtil.isAncestor(VfsUtil.virtualToIoFile(inputData.getFile()), file, false)) {
                return false;
            }
        }

        // dont index files larger then files; use 5 MB here
        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }
}
