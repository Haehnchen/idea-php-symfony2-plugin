package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ImmutableDecoratorService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, ServiceInterface> {

    private static int MAX_FILE_BYTE_SIZE = 5242880;

    public static final ID<String, ServiceInterface> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition_json");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static JsonDataExternalizer JSON_EXTERNALIZER = new JsonDataExternalizer();

    @NotNull
    @Override
    public DataIndexer<String, ServiceInterface, FileContent> getIndexer() {

        return new DataIndexer<String, ServiceInterface, FileContent>() {
            @NotNull
            @Override
            public Map<String, ServiceInterface> map(@NotNull FileContent inputData) {

                Map<String, ServiceInterface> map = new THashMap<String, ServiceInterface>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                for (ServiceInterface service : ServiceContainerUtil.getServicesInFile(psiFile)) {
                    map.put(service.getId(), service);
                }

                return map;
            }

        };
    }

    @NotNull
    @Override
    public ID<String, ServiceInterface> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<ServiceInterface> getValueExternalizer() {
        return JSON_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.getFileType() == XmlFileType.INSTANCE || file.getFileType() == YAMLFileType.YML;
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

    /**
     * com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
     */
    public static class MySetDataExternalizer implements DataExternalizer<String[]> {

        private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

        public synchronized void save(@NotNull DataOutput out, String[] values) throws IOException {

            out.writeInt(values.length);
            for(String value: values) {
                this.myStringEnumerator.save(out, value != null ? value : "");
            }

        }

        public synchronized String[] read(@NotNull DataInput in) throws IOException {
            List<String> list = new ArrayList<String>();
            int r = in.readInt();
            while (r > 0) {
                list.add(this.myStringEnumerator.read(in));
                r--;
            }

            return list.toArray(new String[list.size()]);
        }

    }

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

    private static class JsonDataExternalizer implements DataExternalizer<ServiceInterface> {

        private static final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();
        private static final Gson GSON = new Gson();

        @Override
        public void save(@NotNull DataOutput dataOutput, ServiceInterface fileResource) throws IOException {
            myStringEnumerator.save(dataOutput, GSON.toJson(fileResource));
        }

        @Override
        public ServiceInterface read(@NotNull DataInput in) throws IOException {
            try {
                return GSON.fromJson(myStringEnumerator.read(in), SerializableService.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }
    }

}
