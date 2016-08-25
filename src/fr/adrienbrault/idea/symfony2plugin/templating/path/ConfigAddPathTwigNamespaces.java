package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Extract Twig path of config.yml
 *
 *  twig:
 *    paths:
 *      "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigAddPathTwigNamespaces implements TwigNamespaceExtension {
    private static final Key<CachedValue<Collection<TwigPath>>> CACHE = new Key<>("TWIG_CONFIG_ADD_PATH_CACHE");

    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        CachedValue<Collection<TwigPath>> cache = parameter.getProject().getUserData(CACHE);
        if (cache == null) {
            cache = CachedValuesManager.getManager(parameter.getProject()).createCachedValue(() ->
                    CachedValueProvider.Result.create(getTwigPaths(parameter), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );

            parameter.getProject().putUserData(CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    private Collection<TwigPath> getTwigPaths(@NotNull TwigNamespaceExtensionParameter parameter) {
        VirtualFile configDir = VfsUtil.findRelativeFile(parameter.getProject().getBaseDir(), "app", "config");
        if(configDir == null) {
            return Collections.emptyList();
        }

        Collection<TwigPath> twigPaths = new ArrayList<>();

        // app/config/config*yml
        for (VirtualFile file : configDir.getChildren()) {
            if(!file.getName().startsWith("config.yml")) {
                continue;
            }

            PsiFile psiFile = PsiManager.getInstance(parameter.getProject()).findFile(file);
            if(!(psiFile instanceof YAMLFile)) {
                continue;
            }

            for (Pair<String, String> stringStringPair : TwigHelper.getTwigPathFromYamlConfigResolved((YAMLFile) psiFile)) {
                // default path
                String first = stringStringPair.getFirst();
                if(first == null || first.equals("")) {
                    first = TwigPathIndex.MAIN;
                }

                twigPaths.add(new TwigPath(stringStringPair.getSecond(), first, TwigPathIndex.NamespaceType.ADD_PATH, true));
            }
        }

        return twigPaths;
    }
}
