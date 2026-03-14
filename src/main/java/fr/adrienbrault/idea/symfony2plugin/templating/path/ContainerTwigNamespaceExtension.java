package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Collects Twig paths from the compiled container (e.g., appDevDebugProjectContainer.xml).
 * Path normalization is handled by TwigPathServiceParser.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerTwigNamespaceExtension implements TwigNamespaceExtension {

    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        Project project = parameter.getProject();

        TwigPathServiceParser parser = ServiceXmlParserFactory.getInstance(project, TwigPathServiceParser.class);
        return new ArrayList<>(parser.getTwigPathIndex().getTwigPaths());
    }
}
