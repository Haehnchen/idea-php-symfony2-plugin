package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Extract Twig path of config.yml
 *
 *  twig:
 *    paths:
 *      "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
 *      "%kernel.project_dir%/src/vendor/bundle/Resources/views": core
 *      "%kernel.project_dir%/src/vendor/bundle/Resources/views": !core
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigAddPathTwigNamespaces implements TwigNamespaceExtension {
    private static final Key<CachedValue<Collection<Pair<String, String>>>> CACHE = new Key<>("TWIG_CONFIG_ADD_PATH_CACHE");

    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        Project project = parameter.getProject();

        Collection<Pair<String, String>> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(getTwigPaths(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );

        // TwigPath is not cache able as it right now; we need to build it here
        return cachedValue.stream()
            .map(p -> new TwigPath(p.getFirst(), p.getSecond(), TwigUtil.NamespaceType.ADD_PATH, true))
            .collect(Collectors.toList());
    }

    @NotNull
    private static Collection<Pair<String, String>> getTwigPaths(@NotNull Project project) {
        Collection<Pair<String, String>> twigPathNamespace = new HashSet<>();

        // file config files a eg ".../app/..." or "../packages/..."
        Collection<PsiFile> psiFiles = PsiElementUtils.convertVirtualFilesToPsiFiles(
            project,
            ConfigUtil.getConfigurations(project, "twig")
        );

        for (PsiFile psiFile : psiFiles) {
            if (!(psiFile instanceof YAMLFile)) {
                continue;
            }

            for (Pair<String, String> stringStringPair : TwigUtil.getTwigPathFromYamlConfigResolved((YAMLFile) psiFile)) {
                // default path
                String first = stringStringPair.getFirst();
                if(first == null || first.isEmpty()) {
                    first = TwigUtil.MAIN;
                }

                twigPathNamespace.add(Pair.create(stringStringPair.getSecond(), first));
            }
        }

        return twigPathNamespace;
    }
}
