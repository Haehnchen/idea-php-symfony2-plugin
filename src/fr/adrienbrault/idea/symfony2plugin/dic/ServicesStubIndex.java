package fr.adrienbrault.idea.symfony2plugin.dic;

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
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;
import java.util.Set;

public class ServicesStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.services");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return new DataIndexer<String, Void, FileContent>() {
            @NotNull
            @Override
            public Map<String, Void> map(FileContent inputData) {
                Map<String, Void> map = new THashMap<String, Void>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {
                    Map<String, String> localServiceMap = YamlHelper.getLocalServiceMap(psiFile);

                    if(localServiceMap.size() == 0) {
                        return map;
                    }

                    for(Map.Entry<String, String> entry: localServiceMap.entrySet()) {
                        map.put(entry.getKey(), null);
                    }

                    return map;
                }

                if(psiFile instanceof XmlFile) {
                    Set<String> localServices = XmlHelper.getLocalServices(psiFile);

                    if(localServices.size() == 0) {
                        return map;
                    }

                    for(String entry: localServices) {
                        map.put(entry, null);
                    }

                    return map;

                }

                return map;
            }

        };

    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
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

}



