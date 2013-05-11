package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: Lumbendil
 * Date: 7/04/13
 * Time: 20:02
 * To change this template use File | Settings | File Templates.
 */
@State(
       name = "Settings",
       storages = {
               @Storage(id = "default", file="$PROJECT_CONFIG_DIR$/symfony2.xml", scheme = StorageScheme.DIRECTORY_BASED)
       }
)
public class Settings implements PersistentStateComponent<Settings> {

    public static String DEFAULT_CONTAINER_PATH = "app/cache/dev/appDevDebugProjectContainer.xml";
    public static String DEFAULT_URL_GENERATOR_PATH = "app/cache/dev/appDevUrlGenerator.php";

    public String pathToProjectContainer = DEFAULT_CONTAINER_PATH;
    public String pathToUrlGenerator = DEFAULT_URL_GENERATOR_PATH;

    public boolean symfonyContainerTypeProvider = true;
    public boolean objectRepositoryTypeProvider = false;
    public boolean objectRepositoryResultTypeProvider = false;

    protected Project project;

    public static Settings getInstance(Project project) {
        Settings settings = ServiceManager.getService(project, Settings.class);

        settings.project = project;

        return settings;
    }

    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(Settings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }
}
