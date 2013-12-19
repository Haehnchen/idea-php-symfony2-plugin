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
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, Set<String>> {

    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {

        return new DataIndexer<String, Set<String>, FileContent>() {
            @NotNull
            @Override
            public Map<String, Set<String>> map(FileContent inputData) {

                Map<String, Set<String>> map = new THashMap<String, Set<String>>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
                    return map;
                }
                if (!isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {
                    return getServiceMap(map, (YAMLFile) psiFile);
                }

                if(psiFile instanceof XmlFile) {
                    return getServiceMap(map, (XmlFile) psiFile);

                }

                return map;
            }

            private Map<String, Set<String>> getServiceMap(Map<String, Set<String>> map, XmlFile psiFile) {
                Map<String, String> localServices = XmlHelper.getLocalServiceSet(psiFile);

                if(localServices == null || localServices.size() == 0) {
                    return map;
                }

                for(Map.Entry<String, String> entry: localServices.entrySet()) {
                    if(StringUtils.isNotEmpty(entry.getKey())) {
                        map.put(entry.getKey(), new THashSet<String>(Arrays.asList(entry.getValue())));
                    }
                }

                return map;
            }

            private Map<String, Set<String>> getServiceMap(Map<String, Set<String>> map, YAMLFile psiFile) {
                Map<String, String> localServiceMap = YamlHelper.getLocalServiceMap(psiFile);

                if(localServiceMap.size() == 0) {
                    return map;
                }

                for(Map.Entry<String, String> entry: localServiceMap.entrySet()) {
                    if(StringUtils.isNotEmpty(entry.getKey())) {

                        String className = entry.getValue();
                        if(StringUtils.isBlank(className)) {
                            className = null;
                        }

                        map.put(entry.getKey(), new THashSet<String>(Arrays.asList(className)));
                    }
                }

                return map;
            }

        };
    }

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }


    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<Set<String>> getValueExternalizer() {
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
        return 1;
    }

    /**
     * com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
     */
    private class MySetDataExternalizer implements DataExternalizer<Set<String>> {

        private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

        public synchronized void save(DataOutput out, Set<String> values) throws IOException {
            Set<String> valueStrings = new HashSet<String>();
            for(String valueString: values) {
                if(valueString == null) {
                    valueString = "";
                }
                valueStrings.add(valueString);
            }

            out.writeInt(valueStrings.size());
            String s;
            for (Iterator i$ = valueStrings.iterator(); i$.hasNext(); this.myStringEnumerator.save(out, s)) {
                s = (String) i$.next();
            }

        }

        public synchronized Set<String> read(DataInput in) throws IOException {
            THashSet<String> set = new THashSet<String>();
            int r = in.readInt();
            while (r > 0) {
                set.add(this.myStringEnumerator.read(in));
                r--;
            }
            return set;
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

        return true;
    }

}
