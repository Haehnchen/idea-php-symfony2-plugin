package fr.adrienbrault.idea.symfony2plugin.installer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.jetbrains.php.util.PhpConfigurationUtil;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
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
        return PhpConfigurationUtil.downloadFile(project, component, toDir, "https://get.symfony.com/symfony.phar", "symfony.phar");
    }

    @Nullable
    public static VirtualFile downloadComposer(@Nullable Project project, JComponent component, @Nullable String toDir)
    {
        return PhpConfigurationUtil.downloadFile(project, component, toDir, "https://getcomposer.org/composer.phar", "composer.phar");
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
        return !output.toLowerCase().contains("exception]")
            && (output.toLowerCase().contains("successfully") || output.toLowerCase().contains("run your application"));
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
    public static String[] getCreateComposerSymfonyProjectCommand(@NotNull SymfonyInstallerVersion version, @NotNull String installerPath, @NotNull String newProjectPath, @NotNull String phpPath) {

        List<String> commands = new ArrayList<>();

        commands.add(phpPath);
        commands.add(installerPath);
        commands.add("create-project");
        commands.add(newProjectPath + "/" + PROJECT_SUB_FOLDER);

        // "php symfony demo"
        String selectedVersion = version.getVersion();
        if("website".equals(selectedVersion)) {
            commands.add("symfony/website-skeleton");
        } else if("latest".equals(selectedVersion)) {
            commands.add("symfony/skeleton");
        } else {
            commands.add("symfony/skeleton");
            commands.add("4.2");
        }

        return ArrayUtil.toStringArray(commands);
    }

    @NotNull
    public static String[] getCreateProjectCommand(@NotNull SymfonyInstallerVersion version, @NotNull String installerPath, @NotNull String newProjectPath, @NotNull String phpPath, @Nullable String commandLineOptions) {

        List<String> commands = new ArrayList<>();

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

        List<SymfonyInstallerVersion> symfonyInstallerVersions = new ArrayList<>();

        // prevent adding duplicate version on alias names
        Set<String> aliasBranches = new HashSet<>();

        // get alias version, in most common order
        for(String s : new String[] {"latest", "lts"}) {
            JsonElement asJsonObject = jsonObject.get(s);
            if(asJsonObject == null) {
                continue;
            }

            String asString = asJsonObject.getAsString();
            aliasBranches.add(asString);

            symfonyInstallerVersions.add(new SymfonyInstallerVersion(s, String.format("%s (%s)", asString, s)));
        }


        List<SymfonyInstallerVersion> branches = new ArrayList<>();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            if(!entry.getKey().matches("^\\d+\\.\\d+$")) {
                continue;
            }

            // "2.8.0-dev", "2.8.0-DEV" is not supported
            String asString = entry.getValue().getAsString();
            if(asString.matches(".*[a-zA-Z].*") || aliasBranches.contains(asString)) {
                continue;
            }

            branches.add(new SymfonyInstallerVersion(asString, String.format("%s (%s)", entry.getKey(), asString)));
        }

        branches.sort(Comparator.comparing(SymfonyInstallerVersion::getVersion));

        Collections.reverse(branches);

        symfonyInstallerVersions.addAll(branches);

        // we need reverse order for sorting them on version string
        List<SymfonyInstallerVersion> installableVersions = new ArrayList<>();
        for (JsonElement installable : jsonObject.getAsJsonArray("installable")) {
            installableVersions.add(new SymfonyInstallerVersion(installable.getAsString()));
        }
        Collections.reverse(installableVersions);

        symfonyInstallerVersions.addAll(installableVersions);
        return symfonyInstallerVersions;
    }

    @Nullable
    public static String getDownloadVersions() {

        String userAgent = String.format("%s / %s / Symfony Plugin %s",
            ApplicationInfo.getInstance().getVersionName(),
            ApplicationInfo.getInstance().getBuild(),
            PluginManager.getPlugin(PluginId.getId("fr.adrienbrault.idea.symfony2plugin")).getVersion()
        );

        try {

            // @TODO: PhpStorm9:
            // simple replacement for: com.intellij.util.io.HttpRequests
            URL url = new URL("https://symfony.com/versions.json");
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String content = "";
            String line;
            while ((line = in.readLine()) != null) {
                content += line;
            }

            in.close();

            return content;
        } catch (IOException e) {
            return null;
        }

    }

}
