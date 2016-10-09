package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * FooBundle/Resources/views/foo.html.twig => FooBundle:foo.html.twig
 * FooBundle/Resources/views/foo.html.twig => @Foo/foo.html.twig
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleTwigNamespaceExtension implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        Collection<TwigPath> twigPaths = new ArrayList<>();

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(PhpIndex.getInstance(parameter.getProject())).getBundles();
        for (SymfonyBundle bundle : symfonyBundles) {
            PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(views == null) {
                continue;
            }

            // strip starting backslash to force relative path
            String path = StringUtils.stripStart(views.getVirtualFile().getPath(), "/");

            String bundleName = bundle.getName();

            twigPaths.add(new TwigPath(path, bundleName, TwigPathIndex.NamespaceType.BUNDLE));
            if(bundleName.endsWith("Bundle")) {
                twigPaths.add(new TwigPath(path, bundleName.substring(0, bundleName.length() - 6), TwigPathIndex.NamespaceType.ADD_PATH));
            }
        }

        return twigPaths;
    }
}
