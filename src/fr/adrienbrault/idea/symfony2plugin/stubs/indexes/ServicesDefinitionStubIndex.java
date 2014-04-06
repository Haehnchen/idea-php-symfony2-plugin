package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;


public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, String[]> {

    public static final ID<String, String[]> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, String[], FileContent> getIndexer() {

        return new DataIndexer<String, String[], FileContent>() {
            @NotNull
            @Override
            public Map<String, String[]> map(FileContent inputData) {

                Map<String, String[]> map = new THashMap<String, String[]>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
                    return map;
                }
                if (!isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {
                    attachServiceMap(map, (YAMLFile) psiFile);
                }

                if(psiFile instanceof XmlFile) {
                    attachServiceMap(map, (XmlFile) psiFile);

                }

                return map;
            }

            private void attachServiceMap(Map<String, String[]> map, XmlFile psiFile) {
                attachServiceMap(map, XmlHelper.getLocalServiceMap(psiFile));
            }

            private void attachServiceMap(Map<String, String[]> map, YAMLFile psiFile) {
                attachServiceMap(map, YamlHelper.getLocalServiceMap(psiFile));
            }

            private void attachServiceMap(Map<String, String[]> map, Map<String, ContainerService> localServiceMap) {

                if(localServiceMap.size() == 0) {
                    return;
                }

                for(Map.Entry<String, ContainerService> entry: localServiceMap.entrySet()) {
                    if(StringUtils.isNotBlank(entry.getKey())) {
                        addContainerService(map, entry);
                    }
                }
            }

            private void addContainerService(Map<String, String[]> map, Map.Entry<String, ContainerService> entry) {

                if(StringUtils.isBlank(entry.getKey())) {
                    return;
                }

                String className = entry.getValue().getClassName();
                if(StringUtils.isBlank(className)) {
                    className = null;
                }

                String isPrivate = null;
                if(entry.getValue().isPrivate()) {
                    isPrivate = "true";
                }

                map.put(entry.getKey(), new String[] {className, isPrivate});

            }

        };
    }

    @NotNull
    @Override
    public ID<String, String[]> getName() {
        return KEY;
    }


    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<String[]> getValueExternalizer() {
        return new MySetDataExternalizer();
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(VirtualFile file) {
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
        return 3;
    }

    /**
     * com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
     */
    public static class MySetDataExternalizer implements DataExternalizer<String[]> {

        private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

        public synchronized void save(DataOutput out, String[] values) throws IOException {

            out.writeInt(values.length);
            for(String value: values) {
                this.myStringEnumerator.save(out, value != null ? value : "");
            }

        }

        public synchronized String[] read(DataInput in) throws IOException {
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

        // is test file on path
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), psiFile.getProject().getBaseDir(), '/');
        if(relativePath == null || relativePath.contains("Test")) {
            return false;
        }

        // dont add configured service paths
        List<File> settingsServiceFiles = psiFile.getProject().getComponent(Symfony2ProjectComponent.class).getContainerFiles();
        for(File file: settingsServiceFiles) {
            if(VfsUtil.isAncestor(VfsUtil.virtualToIoFile(inputData.getFile()), file, false)) {
                return false;
            }
        }

        return true;
    }

}
