package fr.adrienbrault.idea.symfony2plugin.routing.webDeployment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.containers.HashMap;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutingRemoteFileStorage implements RemoteFileStorageInterface<Map<String, Route>> {

    private Map<String, Route> routeMap = new HashMap<String, Route>();

    @NotNull
    @Override
    public Collection<String> files(@NotNull Project project) {
        return RemoteWebServerUtil.getRemoteAbleFiles(Settings.getInstance(project).routingFiles);
    }

    @Override
    public void build(@NotNull Project project, @NotNull Collection<FileObject> fileObjects) {
        Map<String, Route> routeMap = new HashMap<String, Route>();

        for (FileObject file : fileObjects) {

            String content;
            try {
                content = StreamUtil.readText(file.getContent().getInputStream(), "UTF-8");
            } catch (IOException e) {
                continue;
            }

            if(StringUtils.isBlank(content)) {
                continue;
            }

            routeMap.putAll(RouteHelper.getRoutesInsideUrlGeneratorFile(
                PsiFileFactory.getInstance(project).createFileFromText("DUMMY__." + PhpFileType.INSTANCE.getDefaultExtension(), PhpFileType.INSTANCE, content)
            ));
        }

        this.routeMap = routeMap;
    }

    @NotNull
    public Map<String, Route> getState() {
        return this.routeMap;
    }

    @Override
    public void clear() {
        this.routeMap = new HashMap<String, Route>();
    }
}
