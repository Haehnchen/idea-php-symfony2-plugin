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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Based on Twig content add overlay to the default Twig file icon, indicating the possible template type
 *
 * Icon positions:
 * - Controller: top left (NORTH_WEST)
 * - Extends: bottom left (SOUTH_WEST)
 * - Include: bottom right (SOUTH_EAST)
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

        // Collect all badge icons with their positions
        List<BadgeIcon> badges = new ArrayList<>();

        // Controller: top left
        if (hasController(twigFile, templateNames)) {
            badges.add(new BadgeIcon(Symfony2Icons.TWIG_CONTROLLER_FILE, SwingConstants.NORTH_WEST));
        }

        // Extends: bottom left
        if (hasFileExtendsTag(project, virtualFile)) {
            badges.add(new BadgeIcon(Symfony2Icons.TWIG_EXTENDS_FILE, SwingConstants.SOUTH_WEST));
        }

        // Include: bottom right
        if (isIncludedByOtherTemplates(project, templateNames)) {
            badges.add(new BadgeIcon(Symfony2Icons.TWIG_IMPLEMENTS_FILE, SwingConstants.SOUTH_EAST));
        }

        if (badges.isEmpty()) {
            return null;
        }

        // Build layered icon
        Icon[] layers = new Icon[badges.size() + 1];
        layers[0] = TwigIcons.TwigFileIcon;
        for (int i = 0; i < badges.size(); i++) {
            layers[i + 1] = badges.get(i).icon;
        }

        LayeredIcon icon = LayeredIcon.layeredIcon(layers);
        for (int i = 0; i < badges.size(); i++) {
            BadgeIcon badge = badges.get(i);
            icon.setIcon(badge.icon, i + 1, badge.position);
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

    /**
     * Holds an icon badge with its target position
     */
    private record BadgeIcon(Icon icon, int position) {}
}
