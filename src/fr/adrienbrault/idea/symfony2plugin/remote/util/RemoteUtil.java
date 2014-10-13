package fr.adrienbrault.idea.symfony2plugin.remote.util;

import fr.adrienbrault.idea.symfony2plugin.remote.provider.DoctrineProvider;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.RouterProvider;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.TwigProvider;

public class RemoteUtil {

    public static Class[] getProviderClasses() {
        return new Class[] {
            DoctrineProvider.class,
            TwigProvider.class,
            RouterProvider.class
        };
    }

}
