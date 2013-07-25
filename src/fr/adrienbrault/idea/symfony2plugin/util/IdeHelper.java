package fr.adrienbrault.idea.symfony2plugin.util;

import java.io.IOException;
import java.net.URISyntaxException;

public class IdeHelper {

    public static void openUrl(String url) {
        if(java.awt.Desktop.isDesktopSupported() ) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            if(desktop.isSupported(java.awt.Desktop.Action.BROWSE) ) {
                try {
                    java.net.URI uri = new java.net.URI(url);
                    desktop.browse(uri);
                } catch (URISyntaxException ignored) {
                } catch (IOException ignored) {

                }
            }
        }
    }



}
