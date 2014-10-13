package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

@State(name = "Symfony2Plugin", storages = @Storage(id = "symfony2-plugin", file = "$APP_CONFIG$/symfony2-plugin.app.xml"))
public class Symfony2ApplicationSettings implements PersistentStateComponent<Symfony2ApplicationSettings> {

    public boolean serverEnabled = false;
    public boolean listenAll = false;
    public int serverPort = 22221;

    @Nullable
    @Override
    public Symfony2ApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(Symfony2ApplicationSettings symfony2ApplicationSettings) {
        XmlSerializerUtil.copyBean(symfony2ApplicationSettings, this);
    }

    public static Symfony2ApplicationSettings getInstance() {
        return ServiceManager.getService(Symfony2ApplicationSettings.class);
    }

}
