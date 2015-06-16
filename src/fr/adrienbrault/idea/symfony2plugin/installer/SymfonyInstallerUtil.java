package fr.adrienbrault.idea.symfony2plugin.installer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.jetbrains.php.util.PhpConfigurationUtil;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerUtil {

    public static String PROJECT_SUB_FOLDER = ".checkout";
    public static String INSTALLER_GROUP_DISPLAY_ID = "Symfony";

    @Nullable
    public static VirtualFile downloadPhar(@Nullable Project project, JComponent component, @Nullable String toDir)
    {
        return PhpConfigurationUtil.downloadFile(project, component, toDir, "http://symfony.com/installer", "symfony.phar");
    }

    @Nullable
    public static String extractSuccessMessage(@NotNull String output)
    {
        Matcher matcher = Pattern.compile("Preparing project[.]*(.*)", Pattern.DOTALL).matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    @NotNull
    public static String formatConsoleTextIndicatorOutput(@NotNull String text) {

        // 2.03 MB/4.95 MB ======>---   41.2%
        Matcher matcher = Pattern.compile("([^=->]*).*[\\s*]([\\d+.]*%)").matcher(text);
        if (matcher.find()) {
            return String.format("%s - %s", matcher.group(2).trim(), matcher.group(1).trim());
        }

        return text;
    }

    public static boolean isSuccessfullyInstalled(@NotNull String output) {
        // successfully installed
        // [RuntimeException]
        return output.toLowerCase().contains("successfully") && !output.toLowerCase().contains("exception]");
    }

    @Nullable
    public static String formatExceptionMessage(@Nullable String output) {

        if(output == null) {
            return null;
        }

        // [RuntimeException] message
        Matcher matcher = Pattern.compile("Exception](.*)", Pattern.DOTALL).matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return output;
    }

    @NotNull
    public static String[] getCreateProjectCommand(@NotNull SymfonyInstallerVersion version, @NotNull String installerPath, @NotNull String newProjectPath, @NotNull String phpPath, @Nullable String commandLineOptions) {

        List<String> commands = new ArrayList<String>();

        commands.add(phpPath);
        commands.add(installerPath);

        // "php symfony demo"
        if("demo".equals(version.getVersion())) {
            commands.add("demo");
            commands.add(newProjectPath + "/" + PROJECT_SUB_FOLDER);
        } else {
            commands.add("new");
            commands.add(newProjectPath + "/" + PROJECT_SUB_FOLDER);
            commands.add(version.getVersion());
        }

        if(commandLineOptions != null) {
            commands.add(commandLineOptions);
        }

        return ArrayUtil.toStringArray(commands);
    }

    @NotNull
    public static List<SymfonyInstallerVersion> getVersions(@NotNull String jsonContent) {

        JsonObject jsonObject = new JsonParser().parse(jsonContent).getAsJsonObject();

        List<SymfonyInstallerVersion> symfonyInstallerVersions = new ArrayList<SymfonyInstallerVersion>();

        // get alias version, in most common order
        for(String s : new String[] {"latest", "lts"}) {
            JsonElement asJsonObject = jsonObject.get(s);
            if(asJsonObject == null) {
                continue;
            }

            String asString = asJsonObject.getAsString();
            symfonyInstallerVersions.add(new SymfonyInstallerVersion(s, String.format("%s (%s)", asString, s)));
        }

        // we need reverse order for sorting them on version string
        List<SymfonyInstallerVersion> installableVersions = new ArrayList<SymfonyInstallerVersion>();
        for (JsonElement installable : jsonObject.getAsJsonArray("installable")) {
            installableVersions.add(new SymfonyInstallerVersion(installable.getAsString()));
        }
        Collections.reverse(installableVersions);

        symfonyInstallerVersions.addAll(installableVersions);
        return symfonyInstallerVersions;
    }

}
