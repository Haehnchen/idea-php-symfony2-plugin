package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
       name = "Settings",
       storages = {
               @Storage(id = "default", file = StoragePathMacros.PROJECT_FILE),
               @Storage(id = "dir", file = StoragePathMacros.PROJECT_CONFIG_DIR + "/symfony2.xml", scheme = StorageScheme.DIRECTORY_BASED)
       }
)
public class Settings implements PersistentStateComponent<Settings> {

    public static String DEFAULT_CONTAINER_PATH = "app/cache/dev/appDevDebugProjectContainer.xml";
    public static String DEFAULT_URL_GENERATOR_PATH = "app/cache/dev/appDevUrlGenerator.php";
    public static String DEFAULT_TRANSLATION_PATH = "app/cache/dev/translations";

    public static String DEFAULT_WEB_DIRECTORY = "web";
    public static String DEFAULT_APP_DIRECTORY = "app";

    public String pathToUrlGenerator = DEFAULT_URL_GENERATOR_PATH;
    public String pathToTranslation = DEFAULT_TRANSLATION_PATH;
    public String directoryToWeb = DEFAULT_WEB_DIRECTORY;
    public String directoryToApp = DEFAULT_APP_DIRECTORY;

    public boolean pluginEnabled = false;

    public boolean symfonyContainerTypeProvider = true;
    public boolean objectRepositoryTypeProvider = true;
    public boolean objectRepositoryResultTypeProvider = true;
    public boolean objectManagerFindTypeProvider = true;

    public boolean twigAnnotateTemplate = true;
    public boolean twigAnnotateAsset = true;
    public boolean twigAnnotateAssetTags = true;
    public boolean twigAnnotateRoute = true;

    public boolean phpAnnotateTemplate = true;
    public boolean phpAnnotateService = true;
    public boolean phpAnnotateRoute = true;
    public boolean phpAnnotateTemplateAnnotation = true;

    public boolean yamlAnnotateServiceConfig = true;

    @Nullable
    public List<TwigNamespaceSetting> twigNamespaces = new ArrayList<TwigNamespaceSetting>();

    @Nullable
    public List<ContainerFile> containerFiles = new ArrayList<ContainerFile>();

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
