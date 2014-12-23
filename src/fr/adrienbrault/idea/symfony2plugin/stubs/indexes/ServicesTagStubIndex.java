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
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;
import java.util.Set;


public class ServicesTagStubIndex extends FileBasedIndexExtension<String, String[]> {

    public static final ID<String, String[]> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_tags");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, String[], FileContent> getIndexer() {

        return new DataIndexer<String, String[], FileContent>() {
            @NotNull
            @Override
            public Map<String, String[]> map(@NotNull FileContent inputData) {

                Map<String, String[]> map = new THashMap<String, String[]>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return map;
                }

                if (!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {
                    this.attachSet(FormUtil.getTags((YAMLFile) psiFile), map);
                }

                if(psiFile instanceof XmlFile) {
                    this.attachSet(FormUtil.getTags((XmlFile) psiFile), map);
                }

                return map;
            }

            private void attachSet(Map<String, Set<String>> source, Map<String, String[]> target) {
                for(Map.Entry<String, Set<String>> entry: source.entrySet()) {
                    Set<String> key = entry.getValue();
                    target.put(entry.getKey(), key.toArray(new String[key.size()]));
                }
            }

        };
    }

    @NotNull
    @Override
    public ID<String, String[]> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<String[]> getValueExternalizer() {
        return new ServicesDefinitionStubIndex.MySetDataExternalizer();
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
        return 3;
    }

}
