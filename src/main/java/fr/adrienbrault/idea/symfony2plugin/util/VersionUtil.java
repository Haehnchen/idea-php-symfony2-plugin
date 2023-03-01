package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.application.ApplicationInfo;

public class VersionUtil {
    public static boolean productVersionGreaterThanOrEqual(int major, int minor) {
        ApplicationInfo instance = ApplicationInfo.getInstance();

        return Integer.parseInt(instance.getMajorVersion()) > major || (Integer.valueOf(instance.getMajorVersion()).equals(major) && Integer.parseInt(instance.getMinorVersionMainPart()) >= minor);
    }
}
