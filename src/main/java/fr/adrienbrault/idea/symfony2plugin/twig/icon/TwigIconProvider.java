package fr.adrienbrault.idea.symfony2plugin.twig.icon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Based on Twig content add overlay to the default Twig file icon, indicating the possible template type
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigIconProvider extends IconProvider {
    public Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
        if (!(element instanceof TwigFile twigFile) || !Settings.getInstance(element.getProject()).featureTwigIcon || !Symfony2ProjectComponent.isEnabled(element.getProject()) || DumbService.getInstance(element.getProject()).isDumb()) {
            return null;
        }

        VirtualFile virtualFile = twigFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Project project = twigFile.getProject();
        Set<String> templateNames = new LinkedHashSet<>(TwigUtil.getTemplateNamesForFile(project, virtualFile));
        if (templateNames.isEmpty()) {
            return null;
        }

        // attach controller icon overlay
        LayeredIcon icon = null;

        if (hasController(twigFile, templateNames)) {
            icon = LayeredIcon.layeredIcon(new Icon[]{TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_CONTROLLER_FILE});
            icon.setIcon(Symfony2Icons.TWIG_CONTROLLER_FILE, 1, SwingConstants.NORTH_WEST);
        }

        // file provides extends tag, add another layer on top; but put the layer below the previous layer if provided
        if (hasFileExtendsTag(project, virtualFile)) {
            if (icon == null) {
                // we are alone so just place the icon
                icon = LayeredIcon.layeredIcon(new Icon[]{TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_EXTENDS_FILE});
                icon.setIcon(Symfony2Icons.TWIG_EXTENDS_FILE, 1, SwingConstants.NORTH_WEST);
            } else {
                // icon should be below first one
                icon.setIcon(Symfony2Icons.TWIG_EXTENDS_FILE, 1, 0, Symfony2Icons.TWIG_CONTROLLER_FILE.getIconHeight() + 1);
            }
        }

        // template is included by other templates, add badge icon in bottom-right corner
        if (isIncludedByOtherTemplates(project, templateNames)) {
            if (icon == null) {
                icon = LayeredIcon.layeredIcon(new Icon[]{TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_IMPLEMENTS_FILE});
                icon.setIcon(Symfony2Icons.TWIG_IMPLEMENTS_FILE, 1, SwingConstants.SOUTH_EAST);
            } else {
                icon.setIcon(Symfony2Icons.TWIG_IMPLEMENTS_FILE, 1, SwingConstants.SOUTH_EAST);
            }
        }

        return icon;
    }

    private boolean hasFileExtendsTag(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return !FileBasedIndex.getInstance().getFileData(TwigExtendsStubIndex.KEY, virtualFile, project).isEmpty();
    }

    private boolean hasController(@NotNull TwigFile twigFile, @NotNull Collection<String> templateNames) {
        return !TwigUtil.getTwigFileMethodUsageOnIndex(twigFile.getProject(), templateNames).isEmpty()
            || !TwigUtil.findTwigFileController(twigFile).isEmpty();
    }

    /**
     * Check if template is included by other templates:
     * {% include 'template.html.twig' %} and {{ include('template.html.twig') }}
     */
    private boolean isIncludedByOtherTemplates(@NotNull Project project, @NotNull Collection<String> templateNames) {
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.allScope(project),
            TwigFileType.INSTANCE
        );

        for (String templateName : templateNames) {
            // Check if any file includes this template (both {% include %} and {{ include() }})
            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigIncludeStubIndex.KEY,
                templateName,
                scope
            );

            if (!containingFiles.isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
