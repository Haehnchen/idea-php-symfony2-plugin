package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ContainerParameterStubIndex extends FileBasedIndexExtension<String, String> {

    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.parameter2");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return new DataIndexer<String, String, FileContent>() {
            @NotNull
            @Override
            public Map<String, String> map(@NotNull FileContent inputData) {
                Map<String, String> map = new HashMap<String, String>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return map;
                }

                if(!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {
                    attachTHashMapNullable(YamlHelper.getLocalParameterMap(psiFile), map);
                }

                if(psiFile instanceof XmlFile) {
                    attachTHashMapNullable(XmlHelper.getFileParameterMap((XmlFile) psiFile), map);
                }

                return map;
            }

        };

    }

    /**
     * workaround for nullable keys #238
     */
    private void attachTHashMapNullable(Map<String, String> source, Map<String, String> tHashMap) {
        for(Map.Entry<String, String> entry: source.entrySet()) {

            // we can remove empty key check now? error is in "value" #238, #277
            String key = entry.getKey();
            if(key != null) {

                // we are not allowed to save null values;
                // but we can have them so provide empty value then
                String value = entry.getValue();
                if(value == null) value = "";

                tHashMap.put(key, value);
            }
        }
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return StringDataExternalizer.STRING_DATA_EXTERNALIZER;
    }

    @NotNull
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

    private static class StringDataExternalizer implements DataExternalizer<String> {

        public static final StringDataExternalizer STRING_DATA_EXTERNALIZER = new StringDataExternalizer();
        private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

        @Override
        public void save(@NotNull DataOutput out, String value) throws IOException {

            if(value == null) {
                value = "";
            }

            this.myStringEnumerator.save(out, value);
        }

        @Override
        public String read(@NotNull DataInput in) throws IOException {

            String value = this.myStringEnumerator.read(in);

            // EnumeratorStringDescriptor writes out "null" as string, so workaround here
            if("null".equals(value)) {
                value = "";
            }

            // it looks like this is our "null keys not supported" #238, #277
            // so dont force null values here

            return value;
        }
    }

}



