package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, ServiceSerializable> {

    private static int MAX_FILE_BYTE_SIZE = 5242880;

    public static final ID<String, ServiceSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static ObjectStreamDataExternalizer<ServiceSerializable> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public DataIndexer<String, ServiceSerializable, FileContent> getIndexer() {

        return inputData -> {

            Map<String, ServiceSerializable> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                return map;
            }

            for (ServiceSerializable service : ServiceContainerUtil.getServicesInFile(psiFile)) {
                map.put(service.getId().toLowerCase(), service);
            }

            return map;
        };
    }

    @NotNull
    @Override
    public ID<String, ServiceSerializable> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<ServiceSerializable> getValueExternalizer() {
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
        return 7;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        // container file need to be xml file, eg xsd filetypes are not valid
        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml") || extension.equalsIgnoreCase("php"))) {
            return false;
        }

        // possible fixture or test file
        // to support also library paths, only filter them on project files
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(inputData.getProject()), '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        // dont add configured service paths
        Collection<File> settingsServiceFiles = psiFile.getProject().getComponent(Symfony2ProjectComponent.class).getContainerFiles();
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
