package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
       name = "Symfony2PluginSettings",
       storages = {
               @Storage(id = "default", file = StoragePathMacros.PROJECT_FILE),
               @Storage(id = "dir", file = StoragePathMacros.PROJECT_CONFIG_DIR + "/symfony2.xml", scheme = StorageScheme.DIRECTORY_BASED)
       }
)
public class Settings implements PersistentStateComponent<Settings> {

    // Symfony 2 and 3 paths
    public static String[] DEFAULT_CONTAINER_PATHS = new String[] {
        "app/cache/dev/appDevDebugProjectContainer.xml",
        "var/cache/dev/appDevDebugProjectContainer.xml",
        "var/cache/dev/srcDevDebugProjectContainer.xml",
    };

    // Symfony 2 and 3 paths
    public static String[] DEFAULT_ROUTES = new String[] {
        "app/cache/dev/appDevUrlGenerator.php",
        "var/cache/dev/appDevUrlGenerator.php",
        "var/cache/dev/srcDevDebugProjectContainerUrlGenerator.php",   
    };

    public static String DEFAULT_TRANSLATION_PATH = "app/cache/dev/translations";

    public static String DEFAULT_WEB_DIRECTORY = "web";
    public static String DEFAULT_APP_DIRECTORY = "app";

    public String pathToTranslation = DEFAULT_TRANSLATION_PATH;
    public String directoryToWeb = DEFAULT_WEB_DIRECTORY;
    public String directoryToApp = DEFAULT_APP_DIRECTORY;
    public String serviceJsNameStrategy = null;

    public boolean remoteDevFileScheduler = false;

    public boolean pluginEnabled = false;

    public boolean symfonyContainerTypeProvider = true;
    public boolean objectRepositoryTypeProvider = true;
    public boolean objectRepositoryResultTypeProvider = true;
    public boolean objectManagerFindTypeProvider = true;

    public boolean objectSignatureTypeProvider = false;

    public boolean twigAnnotateTemplate = true;
    public boolean twigAnnotateAsset = true;
    public boolean twigAnnotateAssetTags = true;
    public boolean twigAnnotateRoute = true;

    public boolean phpAnnotateTemplate = true;
    public boolean phpAnnotateRoute = true;
    public boolean phpAnnotateTemplateAnnotation = true;
    public boolean phpAnnotateTranslation = true;

    public boolean yamlAnnotateServiceConfig = true;

    public boolean codeFoldingPhpRoute = true;
    public boolean codeFoldingPhpModel = true;
    public boolean codeFoldingPhpTemplate = true;
    public boolean codeFoldingTwigRoute = true;
    public boolean codeFoldingTwigTemplate = true;
    public boolean codeFoldingTwigConstant = true;

    public boolean dismissEnableNotification = false;

    public boolean profilerLocalEnabled = false;
    public String profilerLocalUrl = "http://127.0.0.1:8000";
    public String profilerCsvPath;

    public boolean profilerHttpEnabled = false;
    public String profilerHttpUrl = "http://127.0.0.1:8000";

    /**
     * Last user selected output of service generator eg: yaml or xml
     */
    public String lastServiceGeneratorLanguage = null;

    @Nullable
    public List<TwigNamespaceSetting> twigNamespaces = new ArrayList<>();

    @Nullable
    public List<ContainerFile> containerFiles = new ArrayList<>();

    @Nullable
    public List<RoutingFile> routingFiles = new ArrayList<>();

    @Nullable
    public List<MethodParameterSetting> methodParameterSettings = new ArrayList<>();

    @Nullable
    public List<MethodSignatureSetting> methodSignatureSettings = new ArrayList<>();

    public static Settings getInstance(Project project) {
        return ServiceManager.getService(project, Settings.class);
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
