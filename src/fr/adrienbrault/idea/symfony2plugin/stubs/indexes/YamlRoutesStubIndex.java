package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;
import java.util.Set;

public class YamlRoutesStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.yaml_routes");
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
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return map;
                }

                if(psiFile instanceof YAMLFile) {

                    if(!isValidForIndex(inputData, psiFile)) {
                        return map;
                    }

                    YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
                    if(yamlDocument == null) {
                        return map;
                    }

                    Set<String> localServiceMap = RouteHelper.getYamlRouteNames(yamlDocument);
                    if(localServiceMap == null || localServiceMap.size() == 0) {
                        return map;
                    }

                    for(String keyName: localServiceMap) {
                        map.put(keyName, null);
                    }

                    return map;
                }

                if(psiFile instanceof XmlFile) {
                    for(String keyName: RouteHelper.getXmlRouteNames((XmlFile) psiFile)) {
                        map.put(keyName, null);
                    }
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
                return file.getFileType() == YAMLFileType.YML || file.getFileType() == XmlFileType.INSTANCE;
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.contains("Test")) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), psiFile.getProject().getBaseDir(), '/');
        if(relativePath == null || relativePath.contains("Test") || relativePath.contains("Fixtures")) {
            return false;
        }

        return true;
    }



}



