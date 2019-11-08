package fr.adrienbrault.idea.symfonyplugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.HashMap;
import java.util.Map;

public class ContainerParameterStubIndex extends FileBasedIndexExtension<String, String> {

    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfonyplugin.parameter2");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            Map<String, String> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                attachTHashMapNullable(YamlHelper.getLocalParameterMap(psiFile), map);
            } else if(psiFile instanceof XmlFile) {
                attachTHashMapNullable(XmlHelper.getFileParameterMap((XmlFile) psiFile), map);
            }

            return map;
        };
    }

    /**
     * workaround for nullable keys #238
     */
    private void attachTHashMapNullable(@NotNull Map<String, String> source, @NotNull Map<String, String> tHashMap) {
        for(Map.Entry<String, String> entry: source.entrySet()) {

            // dont index null values
            String value = entry.getValue();
            if(value == null) {
                value = "";
            }

            tHashMap.put(entry.getKey(), value);
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
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == XmlFileType.INSTANCE || file.getFileType() == YAMLFileType.YML;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 3;
    }
}



