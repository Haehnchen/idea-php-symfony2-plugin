package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;

public class RoutesStubIndex extends FileBasedIndexExtension<String, StubIndexedRoute> {

    public static final ID<String, StubIndexedRoute> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.routes_object");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static ObjectStreamDataExternalizer<StubIndexedRoute> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, StubIndexedRoute> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, StubIndexedRoute, FileContent> getIndexer() {
        return inputData -> {
            Map<String, StubIndexedRoute> map = new THashMap<>();

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

                for(StubIndexedRoute indexedRoutes: RouteHelper.getYamlRouteDefinitions(yamlDocument)) {
                    map.put(indexedRoutes.getName(), indexedRoutes);
                }

                return map;
            }

            if(psiFile instanceof XmlFile) {
                for(StubIndexedRoute indexedRoutes: RouteHelper.getXmlRouteDefinitions((XmlFile) psiFile)) {
                    map.put(indexedRoutes.getName(), indexedRoutes);
                }
            }

            return map;
        };

    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<StubIndexedRoute> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file ->
            file.getFileType() == YAMLFileType.YML || file.getFileType() == XmlFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 3;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.contains("Test")) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), psiFile.getProject().getBaseDir(), '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        return true;
    }
}



