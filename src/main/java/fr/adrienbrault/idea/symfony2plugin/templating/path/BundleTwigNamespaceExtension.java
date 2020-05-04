package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
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

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(parameter.getProject()).getBundles();
        for (SymfonyBundle bundle : symfonyBundles) {
            PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(views == null) {
                continue;
            }

            // @TODO: use relative path and make os independent
            String path = views.getVirtualFile().getPath();

            String bundleName = bundle.getName();

            twigPaths.add(new TwigPath(path, bundleName, TwigUtil.NamespaceType.BUNDLE));

            // Bundle is stripped from its name on the ending "FooBarBundle" => "FooBarBundle"
            if (bundleName.endsWith("Bundle")) {
                bundleName = bundleName.substring(0, bundleName.length() - 6);
            }

            twigPaths.add(new TwigPath(path, bundleName, TwigUtil.NamespaceType.ADD_PATH));
        }

        return twigPaths;
    }
}
