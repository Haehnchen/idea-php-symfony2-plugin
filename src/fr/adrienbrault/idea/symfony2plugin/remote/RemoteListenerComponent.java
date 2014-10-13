package fr.adrienbrault.idea.symfony2plugin.remote;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.ui.Messages;
import com.sun.net.httpserver.HttpServer;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ApplicationSettings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.remote.httpHandler.InfoActionHandler;
import fr.adrienbrault.idea.symfony2plugin.remote.httpHandler.ProjectActionHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RemoteListenerComponent implements ApplicationComponent {

    private final Symfony2ApplicationSettings settings;
    private HttpServer server;
    private Thread listenerThread;

    public RemoteListenerComponent(Symfony2ApplicationSettings settings) {
        this.settings = settings;
    }

    public void initComponent() {


        if(!settings.serverEnabled) {
            return;
        }

        final int port = Symfony2ApplicationSettings.getInstance().serverPort;

        try {
            server = HttpServer.create(new InetSocketAddress(!Symfony2ApplicationSettings.getInstance().listenAll ? "localhost" : "0.0.0.0", port), 0);
        } catch (IOException e) {

            Symfony2ProjectComponent.getLogger().error(String.format("Cant start server on %s", port));

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    Messages.showMessageDialog("Can't bind with " + port + " port. RemoteCall plugin won't work", "RemoteCall Plugin Error",
                        Messages.getErrorIcon());
                }
            });

            return;
        }

        server.createContext("/project", new ProjectActionHandler());
        server.createContext("/", new InfoActionHandler());

        final HttpServer finalServer = server;
        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                finalServer.start();
                Symfony2ProjectComponent.getLogger().info(String.format("Starting server on %s", port));
            }
        });

        listenerThread.start();
    }

    public void disposeComponent() {

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        if(server != null) {
            server.stop(0);
        }

    }

    @NotNull
    public String getComponentName() {
        return "RemoteStorage";
    }
}