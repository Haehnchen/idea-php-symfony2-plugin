package fr.adrienbrault.idea.symfony2plugin.installer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.Key;
import com.intellij.util.ArrayUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
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
        // You should fix the reported issues before
        // [KO] PHP extension "pdo_pgsql" not found, please install it - required
        return (output.toLowerCase().contains("[ok]") || output.toLowerCase().contains("successfully"))
            && !output.toLowerCase().contains("exception]")
            && !output.toLowerCase().contains("[nok]")
            && !output.toLowerCase().contains("fix the reported")
        ;
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
    public static String[] getCreateProjectCommand(@NotNull SymfonyInstallerVersion version, @NotNull String installerPath, @NotNull String newProjectPath, @NotNull String projectType) {
        List<String> commands = new ArrayList<>();

        commands.add(installerPath);
        commands.add("new");

        String version1 = version.getVersion();
        if (List.of("lts", "stable", "previous").contains(version1)) {
            commands.add("--version=" + version1);
        }

        if (List.of("full", "demo", "webapp", "book").contains(projectType)) {
            commands.add("--" + projectType);
        }

        commands.add(newProjectPath);

        return ArrayUtil.toStringArray(commands);
    }

    @Nullable
    public static String getReleaseUrl() {
        String contents1;
        try {
            java.io.InputStream is = new java.net.URL("https://api.github.com/repos/symfony-cli/symfony-cli/releases").openStream();
            contents1 = new String(is.readAllBytes());
        } catch (IOException e) {
            return null;
        }

        String lastReleaseUrl = null;
        for (JsonElement jsonElement : com.google.gson.JsonParser.parseString(contents1).getAsJsonArray()) {
            if (!(jsonElement instanceof JsonObject)) {
                continue;
            }

            String url = ((JsonObject) jsonElement).get("assets_url").getAsString();
            if (url != null) {
                lastReleaseUrl = url;
            }
        }

        System.out.println(lastReleaseUrl);

        String contents;
        try {
            java.io.InputStream is = new java.net.URL(lastReleaseUrl + "?per_page=100").openStream();
            contents = new String(is.readAllBytes());
        } catch (IOException e) {
            return null;
        }

        com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(contents);

        Map<String, Integer> archMap = new HashMap<>();
        archMap.put("x86", 32);
        archMap.put("i386", 32);
        archMap.put("i486", 32);
        archMap.put("i586", 32);
        archMap.put("i686", 32);
        archMap.put("x86_64", 64);
        archMap.put("amd64", 64);

        Integer osArch = archMap.get(SystemUtils.OS_ARCH);

        for (JsonElement jsonElement : element.getAsJsonArray()) {
            if (!(jsonElement instanceof JsonObject)) {
                continue;
            }

            String name = ((JsonObject) jsonElement).get("name").getAsString();
            if (!name.endsWith(".tar.gz")) {
                continue;
            }

            String browserDownloadUrl = ((JsonObject) jsonElement).get("browser_download_url").getAsString();
            if (browserDownloadUrl == null) {
                continue;
            }

            if (SystemUtils.IS_OS_LINUX && name.contains("_linux_")) {
                if (osArch == 32 && name.contains("_386")) {
                    return browserDownloadUrl;
                } else if (osArch == 64 && name.contains("amd64")) {
                    return browserDownloadUrl;
                }
            } else if(SystemUtils.IS_OS_WINDOWS && name.contains("_windows_")) {
                if (osArch == 32 && name.contains("_386")) {
                    return browserDownloadUrl;
                } else if (osArch == 64 && name.contains("amd64")) {
                    return browserDownloadUrl;
                }
            } else if(SystemUtils.IS_OS_MAC && name.contains("_darwin_") && name.contains("_all_")) {
                return browserDownloadUrl;
            }
        }

        return null;
    }

    @Nullable
    public static String extractTarGZ(@NotNull String releaseUrl, @NotNull String projectDir) {
        try {
            BufferedInputStream bi = new BufferedInputStream(new URL(releaseUrl).openStream());
            GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
            TarArchiveInputStream ti = new TarArchiveInputStream(gzi);

            ArchiveEntry entry;
            while ((entry = ti.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("symfony") || name.equals("symfony.exe")) {
                    String filePath = projectDir + "/" + name;
                    Files.copy(ti, Path.of(filePath));

                    File file = new File(filePath);
                    file.setExecutable(true);

                    return filePath;
                }
            }
        } catch (IOException e) {
            Symfony2ProjectComponent.getLogger().warn("Symfony CLI: can not fetch release binary: " + e.getMessage());
        }

        return null;
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
            PluginManagerCore.getPlugin(PluginId.getId("fr.adrienbrault.idea.symfony2plugin")).getVersion()
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

    public static boolean isValidSymfonyCliToolsCommand(@NotNull String binaryPath) {
        String[] myCommand = new String[] {binaryPath, "-V"};

        final StringBuilder outputBuilder = new StringBuilder();
        OSProcessHandler processHandler = null;
        try {
            processHandler = ScriptRunnerUtil.execute(myCommand[0], null, null, Arrays.copyOfRange(myCommand, 1, myCommand.length));
        } catch (ExecutionException e) {
            return false;
        }

        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                outputBuilder.append(text);
            }
        });

        processHandler.startNotify();
        if (!processHandler.waitFor(1000 * 5)) {
            return false;
        }

        return outputBuilder.toString().toLowerCase().contains("version");
    }

    public static boolean isValidSymfonyCliToolsCommandInPath() {
        return isValidSymfonyCliToolsCommand("symfony");
    }
}
