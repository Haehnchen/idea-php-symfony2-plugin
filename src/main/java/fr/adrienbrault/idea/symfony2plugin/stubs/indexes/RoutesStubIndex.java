package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AnnotationRouteElementWalkingVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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
                    String name = indexedRoutes.getName();
                    if (name.length() < 255) {
                        map.put(name, indexedRoutes);
                    }
                }

                return map;
            } else if(psiFile instanceof XmlFile) {
                for(StubIndexedRoute indexedRoutes: RouteHelper.getXmlRouteDefinitions((XmlFile) psiFile)) {
                    String name = indexedRoutes.getName();
                    if (name.length() < 255) {
                        map.put(name, indexedRoutes);
                    }
                }
            } else if(psiFile instanceof PhpFile) {
                // annotations: @Route()
                if(!isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                psiFile.accept(new AnnotationRouteElementWalkingVisitor(map));
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
        return FileInputFilter.XML_YAML_PHP;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    private static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        VirtualFile baseDir = ProjectUtil.getProjectDir(inputData.getProject());
        if(baseDir == null) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), baseDir, '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        return true;
    }
}



