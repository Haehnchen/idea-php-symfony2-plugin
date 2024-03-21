package fr.adrienbrault.idea.symfony2plugin.twig.icon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Based on Twig content add overlay to the default Twig file icon, indicating the possible template type
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigIconProvider extends IconProvider {
    public Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
        if (!(element instanceof TwigFile) || !Settings.getInstance(element.getProject()).featureTwigIcon || !Symfony2ProjectComponent.isEnabled(element.getProject()) || DumbService.getInstance(element.getProject()).isDumb()) {
            return null;
        }

        // attach controller icon overlay
        LayeredIcon icon = null;
        if (hasController((TwigFile) element)) {
            icon = new LayeredIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_CONTROLLER_FILE);
            icon.setIcon(Symfony2Icons.TWIG_CONTROLLER_FILE, 1, SwingConstants.NORTH_WEST);
        }

        // file provides extends tag, add another layer on top; but put the layer below the previous layer if provided
        if (hasFileExtendsTag(element)) {
            if (icon == null) {
                // we are alone so just place the icon
                icon = new LayeredIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_EXTENDS_FILE);
                icon.setIcon(Symfony2Icons.TWIG_EXTENDS_FILE, 1, SwingConstants.NORTH_WEST);
            } else {
                // icon should be below first one
                icon = new LayeredIcon(icon, Symfony2Icons.TWIG_IMPLEMENTS_FILE);
                icon.setIcon(Symfony2Icons.TWIG_EXTENDS_FILE, 1, 0, Symfony2Icons.TWIG_CONTROLLER_FILE.getIconHeight() + 1);
            }
        }

        return icon;
    }

    private boolean hasFileExtendsTag(@NotNull PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (child instanceof TwigExtendsTag) {
                return true;
            }

            for (TwigFileUsage extension : TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
                if (extension.isExtendsTemplate(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasController(@NotNull TwigFile twigFile) {
        return !TwigUtil.findTwigFileController(twigFile).isEmpty()
            || !TwigUtil.getTwigFileMethodUsageOnIndex(twigFile).isEmpty();
    }
}
