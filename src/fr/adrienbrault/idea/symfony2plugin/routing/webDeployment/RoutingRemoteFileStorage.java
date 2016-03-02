package fr.adrienbrault.idea.symfony2plugin.routing.webDeployment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutingRemoteFileStorage implements RemoteFileStorageInterface<Map<String, Route>> {

    private Map<String, Route> routeMap = new HashMap<String, Route>();

    @Override
    public Collection<String> files(@NotNull Project project) {
        List<RoutingFile> routingFiles = Settings.getInstance(project).routingFiles;
        if(routingFiles == null) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(ContainerUtil.filter(routingFiles, new Condition<RoutingFile>() {
            @Override
            public boolean value(RoutingFile routingFile) {
                return routingFile.getPath().startsWith("remote://");
            }
        }), new Function<RoutingFile, String>() {
            @Override
            public String fun(RoutingFile routingFile) {
                return routingFile.getPath().substring("remote://".length());
            }
        });
    }

    @Override
    public void build(@NotNull Project project, @NotNull Collection<String> content) {

        Map<String, Route> routeMap = new HashMap<String, Route>();

        for (String s : content) {
            routeMap.putAll(RouteHelper.getRoutesInsideUrlGeneratorFile(
                PsiFileFactory.getInstance(project).createFileFromText("DUMMY__." + PhpFileType.INSTANCE.getDefaultExtension(), PhpFileType.INSTANCE, s)
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
