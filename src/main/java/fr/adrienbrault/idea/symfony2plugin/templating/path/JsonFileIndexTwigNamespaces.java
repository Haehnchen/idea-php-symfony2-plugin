package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigConfigJson;
import fr.adrienbrault.idea.symfony2plugin.templating.path.dict.TwigPathJson;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JsonFileIndexTwigNamespaces implements TwigNamespaceExtension {
    private static final Key<CachedValue<Collection<TwigPath>>> CACHE = new Key<>("TWIG_JSON_INDEX_CACHE");

    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(final @NotNull TwigNamespaceExtensionParameter parameter) {
        Project project = parameter.getProject();

        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(getNamespacesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static Collection<TwigPath> getNamespacesInner(@NotNull Project project) {
        Collection<TwigPath> twigPaths = new ArrayList<>();

        for (final PsiFile psiFile : FilenameIndex.getFilesByName(project, "ide-twig.json", GlobalSearchScope.allScope(project))) {
            Collection<TwigPath> cachedValue = CachedValuesManager.getCachedValue(psiFile, new MyJsonCachedValueProvider(psiFile));
            if(cachedValue != null) {
                twigPaths.addAll(cachedValue);
            }
        }

        return twigPaths;
    }

    private record MyJsonCachedValueProvider(PsiFile psiFile) implements CachedValueProvider<Collection<TwigPath>> {
        @Override
        public Result<Collection<TwigPath>> compute() {
            Collection<TwigPath> twigPaths = new ArrayList<>();

            String text = psiFile.getText();
            TwigConfigJson configJson = null;
            try {
                configJson = new Gson().fromJson(text, TwigConfigJson.class);
            } catch (JsonSyntaxException | JsonIOException | IllegalStateException ignored) {
            }

            if (configJson == null) {
                return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
            }

            for (TwigPathJson twigPath : configJson.getNamespaces()) {
                String path = twigPath.getPath();
                if (path == null || path.equals(".")) {
                    path = "";
                }

                path = StringUtils.stripStart(path.replace("\\", "/"), "/");
                PsiDirectory parent = psiFile.getParent();
                if (parent == null) {
                    continue;
                }

                // current directory check and subfolder
                VirtualFile twigRoot;
                if (path.length() > 0) {
                    twigRoot = VfsUtil.findRelativeFile(parent.getVirtualFile(), path.split("/"));
                } else {
                    twigRoot = psiFile.getParent().getVirtualFile();
                }

                if (twigRoot == null) {
                    continue;
                }

                String relativePath = VfsExUtil.getRelativeProjectPath(psiFile.getProject(), twigRoot);
                if (relativePath == null) {
                    continue;
                }

                String namespace = twigPath.getNamespace();

                TwigUtil.NamespaceType pathType = TwigUtil.NamespaceType.ADD_PATH;
                String type = twigPath.getType();
                if ("bundle".equalsIgnoreCase(type)) {
                    pathType = TwigUtil.NamespaceType.BUNDLE;
                }

                String namespacePath = StringUtils.stripStart(relativePath, "/");

                if (StringUtils.isNotBlank(namespace)) {
                    twigPaths.add(new TwigPath(namespacePath, namespace, pathType, true));
                } else {
                    twigPaths.add(new TwigPath(namespacePath, TwigUtil.MAIN, pathType, true));
                }
            }

            return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
        }
    }
}
