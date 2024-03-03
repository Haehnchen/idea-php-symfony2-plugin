package fr.adrienbrault.idea.symfony2plugin.templating.webpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyWebpackUtil {
    /**
     * - webpack.config.js
     * - entrypoints.json
     */
    public static void visitAllEntryFileTypes(@NotNull Project project, @NotNull Consumer<WebpackAsset> consumer) {
        for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName("webpack.config.js", GlobalSearchScope.allScope(project))) {
            if (!isTestFile(project, virtualFile)) {
                visitWebpackConfiguration(project, virtualFile, consumer);
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
     *     "build/app.js": "/build/app.123abc.js",
     *     "build/dashboard.css": "/build/dashboard.a4bf2d.css"
     * }
     */
    public static void visitManifestJsonEntries(@NotNull VirtualFile virtualFile, @NotNull Consumer<Pair<String, String>> consumer) {
        visitManifestJson(virtualFile, consumer);
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
    private static void visitEntryPointJson(@NotNull VirtualFile virtualFile, @NotNull Consumer<WebpackAsset> consumer) {
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
            consumer.accept(new WebpackAsset(virtualFile, s1));
        }
    }

    /**
     * .addEntry('foobar', './assets/app.js')
     * .addEntry("foo")
     */
    private static void visitWebpackConfiguration(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull Consumer<WebpackAsset> consumer) {
        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        if (file == null) {
            return;
        }

        file.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof JSCallExpression) {
                    PsiElement methodExpression = element.getFirstChild();
                    if (methodExpression instanceof JSReferenceExpression jsReferenceExpression) {
                        String name = jsReferenceExpression.getReferenceName();
                        if ("addStyleEntry".equals(name) || "addEntry".equals(name)) {
                            JSExpression[] arguments = ((JSCallExpression) element).getArguments();
                            if (arguments.length >= 1 && arguments[0] instanceof JSLiteralExpression jsLiteralExpressionArg1) {
                                String parameter1 = jsLiteralExpressionArg1.getStringValue();
                                String parameter2 = null;

                                if (StringUtils.isNotBlank(parameter1)) {
                                    if (arguments.length >= 2 && arguments[1] instanceof JSLiteralExpression jsLiteralExpressionArg2) {
                                        String parameter2Value = jsLiteralExpressionArg2.getStringValue();
                                        if (StringUtils.isNotBlank(parameter2Value)) {
                                            parameter2 = parameter2Value;
                                        }
                                    }

                                    consumer.accept(new WebpackAsset(virtualFile, parameter1, parameter2, arguments[0]));
                                }
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * {
     *     "build/app.js": "/build/app.123abc.js",
     *     "build/dashboard.css": "/build/dashboard.a4bf2d.css"
     * }
     */
    private static void visitManifestJson(@NotNull VirtualFile virtualFile, @NotNull Consumer<Pair<String, String>> consumer) {
        JsonElement jsonElement;
        try {
            String s = StreamUtil.readText(virtualFile.getInputStream(), "UTF-8");
            jsonElement = JsonParser.parseString(s);
        } catch (JsonSyntaxException | IOException e) {
            return;
        }

        if (!(jsonElement instanceof JsonObject))  {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }

            JsonElement value = entry.getValue();

            String content = null;
            if (value.isJsonPrimitive()) {
                content = value.getAsString();
            }

            consumer.accept(Pair.create(key, content));
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

    public static class WebpackAsset {
        private final VirtualFile virtualFile;
        private final String entry;
        private final String entryTarget;
        private final PsiElement psiElement;

        public WebpackAsset(@NotNull VirtualFile virtualFile, @NotNull String entry, @Nullable String entryTarget, @Nullable PsiElement psiElement) {
            this.virtualFile = virtualFile;
            this.entry = entry;
            this.entryTarget = entryTarget;
            this.psiElement = psiElement;
        }

        public WebpackAsset(@NotNull VirtualFile virtualFile, @NotNull String entry) {
            this(virtualFile, entry, null, null);
        }

        public VirtualFile getVirtualFile() {
            return virtualFile;
        }

        public String getEntry() {
            return entry;
        }

        public String getEntryTarget() {
            return entryTarget;
        }

        public PsiElement getPsiElement() {
            return psiElement;
        }
    }
}
