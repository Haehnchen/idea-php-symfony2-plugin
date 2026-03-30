package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.SerializableServiceExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, ServiceSerializable> {

    private static final int MAX_FILE_BYTE_SIZE = 5242880;

    public static final ID<String, ServiceSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final SerializableServiceExternalizer EXTERNALIZER = SerializableServiceExternalizer.INSTANCE;

    @NotNull
    @Override
    public DataIndexer<String, ServiceSerializable, FileContent> getIndexer() {

        return inputData -> {

            Map<String, ServiceSerializable> map = new HashMap<>();

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
        return 10;
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

        // exclude settings-configured service container paths (lightweight path comparison, no VFS lookups)
        List<ContainerFile> settingsContainerFiles = Settings.getInstance(psiFile.getProject()).containerFiles;
        if (settingsContainerFiles != null) {
            String inputPath = FileUtil.toSystemIndependentName(inputData.getFile().getPath());
            String inputRelativePath = relativePath != null ? FileUtil.toSystemIndependentName(relativePath) : null;
            for (ContainerFile containerFile : settingsContainerFiles) {
                String path = containerFile.getPath();
                if (path != null) {
                    String normalizedPath = FileUtil.toSystemIndependentName(path);
                    if (FileUtil.pathsEqual(inputPath, normalizedPath) || (inputRelativePath != null && FileUtil.pathsEqual(inputRelativePath, normalizedPath))) {
                        return false;
                    }
                }
            }
        }

        // exclude XML files in var/cache and app/cache directories
        if (relativePath != null && extension.equalsIgnoreCase("xml")) {
            String lowerPath = FileUtil.toSystemIndependentName(relativePath).toLowerCase(Locale.ROOT);
            if (lowerPath.startsWith("var/cache/") || lowerPath.startsWith("app/cache/")) {
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
