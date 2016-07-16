package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.apache.commons.lang.StringUtils;
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

        CachedValue<Collection<TwigPath>> cache = parameter.getProject().getUserData(CACHE);
        if (cache == null) {
            cache = CachedValuesManager.getManager(parameter.getProject()).createCachedValue(new CachedValueProvider<Collection<TwigPath>>() {
                @Nullable
                @Override
                public Result<Collection<TwigPath>> compute() {
                    return Result.create(getNamespacesInner(parameter), PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);

            parameter.getProject().putUserData(CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    private Collection<TwigPath> getNamespacesInner(@NotNull TwigNamespaceExtensionParameter parameter) {

        Collection<TwigPath> twigPaths = new ArrayList<>();

        for (final PsiFile psiFile : FilenameIndex.getFilesByName(parameter.getProject(), "ide-twig.json", GlobalSearchScope.allScope(parameter.getProject()))) {
            Collection<TwigPath> cachedValue = CachedValuesManager.getCachedValue(psiFile, new MyJsonCachedValueProvider(psiFile));
            if(cachedValue != null) {
                twigPaths.addAll(cachedValue);
            }
        }

        return twigPaths;
    }

    private static class MyJsonCachedValueProvider implements CachedValueProvider<Collection<TwigPath>> {
        private final PsiFile psiFile;

        public MyJsonCachedValueProvider(PsiFile psiFile) {
            this.psiFile = psiFile;
        }

        @Nullable
        @Override
        public Result<Collection<TwigPath>> compute() {

            Collection<TwigPath> twigPaths = new ArrayList<>();

            String text = psiFile.getText();
            TwigConfigJson configJson = null;
            try {
                configJson = new Gson().fromJson(text, TwigConfigJson.class);
            } catch (JsonSyntaxException ignored) {
            } catch (JsonIOException ignored) {
            } catch (IllegalStateException ignored) {
            }

            if(configJson == null) {
                return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
            }

            for(TwigPathJson twigPath : configJson.getNamespaces()) {
                String path = twigPath.getPath();
                if(path == null) {
                    path = "";
                }

                path = StringUtils.stripStart(path.replace("\\", "/"), "/");
                PsiDirectory parent = psiFile.getParent();
                if(parent == null) {
                    continue;
                }

                // current directory check and subfolder
                VirtualFile twigRoot;
                if(path.length() > 0) {
                    twigRoot = VfsUtil.findRelativeFile(parent.getVirtualFile(), path.split("/"));
                } else {
                    twigRoot = psiFile.getParent().getVirtualFile();
                }

                if(twigRoot == null) {
                    continue;
                }

                String relativePath = VfsExUtil.getRelativeProjectPath(psiFile.getProject(), twigRoot);
                if(relativePath == null) {
                    continue;
                }

                String namespace = twigPath.getNamespace();

                TwigPathIndex.NamespaceType pathType = TwigPathIndex.NamespaceType.ADD_PATH;
                String type = twigPath.getType();
                if("bundle".equalsIgnoreCase(type)) {
                    pathType = TwigPathIndex.NamespaceType.BUNDLE;
                }

                String namespacePath = StringUtils.stripStart(relativePath, "/");

                if(StringUtils.isNotBlank(namespace)) {
                    twigPaths.add(new TwigPath(namespacePath, namespace, pathType, true));
                } else {
                    twigPaths.add(new TwigPath(namespacePath, TwigPathIndex.MAIN, pathType, true));
                }
            }

            return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
        }
    }
}
