package fr.adrienbrault.idea.symfony2plugin.templating.webpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyWebpackUtil {

    /**
     * - webpack.config.js
     * - entrypoints.json
     */
    public static void visitAllEntryFileTypes(@NotNull Project project, @NotNull Consumer<Pair<VirtualFile, String>> consumer) {
        for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName("webpack.config.js", GlobalSearchScope.allScope(project))) {
            if (!isTestFile(project, virtualFile)) {
                visitWebpackConfiguration(virtualFile, consumer);
            }
        }

        for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName("entrypoints.json", GlobalSearchScope.allScope(project))) {
            if (!isTestFile(project, virtualFile)) {
                visitEntryPointJson(virtualFile, consumer);
            }
        }
    }

    /**
     * {
     *   "entrypoints": {
     *     "entry_foobar": {
     *       "js": []
     *     },
     *     "entry_foobar_2": {
     *       "js": []
     *     }
     *   }
     * }
     */
    private static void visitEntryPointJson(@NotNull VirtualFile virtualFile, @NotNull Consumer<Pair<VirtualFile, String>> consumer) {
        String s;

        try {
            s = StreamUtil.readText(virtualFile.getInputStream(), "UTF-8");
        } catch (IOException e) {
            return;
        }

        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(s).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return;
        }

        if (jsonObject == null) {
            return;
        }

        JsonElement entrypoints = jsonObject.get("entrypoints");
        if (entrypoints == null) {
            return;
        }

        JsonObject asJsonObject = entrypoints.getAsJsonObject();
        if (asJsonObject == null) {
            return;
        }

        for (String s1 : asJsonObject.keySet()) {
            consumer.accept(Pair.create(virtualFile, s1));
        }
    }

    /**
     * .addEntry('foobar', './assets/app.js')
     * .addEntry("foo")
     */
    private static void visitWebpackConfiguration(@NotNull VirtualFile virtualFile, @NotNull Consumer<Pair<VirtualFile, String>> consumer) {
        String s;

        try {
            s = StreamUtil.readText(virtualFile.getInputStream(), "UTF-8");
        } catch (IOException e) {
            return;
        }

        // if adding "javascript" plugin; would resolve better but not for now
        Matcher matcher = Pattern.compile("(addEntry|addStyleEntry)\\s*\\(\\s*['\"]([^'\"]+)['\"]").matcher(s);
        while(matcher.find()){
            consumer.accept(Pair.create(virtualFile, matcher.group(2)));
        }
    }

    private static boolean isTestFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // ignore: "vendor/symonfy/.../tests/fixtures/build/entrypoints.json"
        String path = VfsUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
        if (path != null) {
            String lowerCase = path.toLowerCase();
            return lowerCase.contains("/test/") || lowerCase.contains("/tests/");
        }

        return false;
    }
}
