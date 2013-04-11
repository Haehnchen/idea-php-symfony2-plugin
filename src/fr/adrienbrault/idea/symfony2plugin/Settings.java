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
    public String pathToProjectContainer = "app/cache/dev/appDevDebugProjectContainer.xml";
    protected Project project;

    public static Settings getInstance(Project project)
    {
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
