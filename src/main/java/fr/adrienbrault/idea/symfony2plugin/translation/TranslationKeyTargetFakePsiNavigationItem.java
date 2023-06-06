package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.icons.AllIcons;
import com.intellij.json.psi.JsonFile;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SizedIcon;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import icons.PhpIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyTargetFakePsiNavigationItem extends GoToSymbolProvider.BaseNavigationItem {
    private final VirtualFile projectDir;
    private final PsiElement myPsiElement;
    private final String rootDir;

    public TranslationKeyTargetFakePsiNavigationItem(@NotNull VirtualFile projectDir, @NotNull PsiElement psiElement) {
        super(psiElement, "", null);

        this.projectDir = projectDir;
        this.rootDir = VfsUtil.getRelativePath(psiElement.getContainingFile().getVirtualFile(), projectDir, '/');
        this.myPsiElement = psiElement;
    }

    @Override
    public Icon getIcon(boolean flags) {
        return getFileIcon();
    }

    public int getWeight() {
        if (("/" + rootDir).startsWith("/translations/")) {
            return -1;
        }

        return isSymfonyTranslationDirectory() ? 1 : 0;
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                return myPsiElement.getContainingFile().getName();
            }

            @Override
            public String getLocationString() {
                String relativePath = VfsUtil.getRelativePath(myPsiElement.getContainingFile().getVirtualFile(), projectDir, '/');
                if (relativePath != null) {
                    return '(' + relativePath + ')';
                }

                return '(' + myPsiElement.getContainingFile().getVirtualFile().toString() + ')';
            }

            @Override
            @Nullable
            public Icon getIcon(boolean open) {
                return TranslationKeyTargetFakePsiNavigationItem.this.getIcon(ICON_FLAG_VISIBILITY);
            }
        };
    }

    private Icon getFileIcon() {
        Icon fileIcon;

        PsiFile containingFile = myPsiElement.getContainingFile();
        if (containingFile instanceof YAMLFile) {
            fileIcon = AllIcons.FileTypes.Yaml;
        } else if (containingFile instanceof PhpFile) {
            fileIcon =  PhpIcons.PhpIcon;
        } else if (containingFile instanceof XmlFile) {
            fileIcon =  AllIcons.FileTypes.Xml;
        } else if (containingFile instanceof JsonFile) {
            fileIcon =  AllIcons.FileTypes.Json;
        } else {
            // for non registered state
            String extension = myPsiElement.getContainingFile().getVirtualFile().getExtension();

            fileIcon = "xlf".equalsIgnoreCase(extension) || "xliff".equalsIgnoreCase(extension)
                ? AllIcons.FileTypes.Xml
                : myPsiElement.getIcon(ICON_FLAG_VISIBILITY);
        }

        if (rootDir != null && isSymfonyTranslationDirectory()) {
            SizedIcon sizedIcon = new SizedIcon(Symfony2Icons.SYMFONY_LINE_MARKER, fileIcon.getIconWidth() / 2, fileIcon.getIconHeight() / 2);
            LayeredIcon icon = new LayeredIcon(fileIcon, sizedIcon);
            icon.setIcon(sizedIcon, 1, SwingConstants.SOUTH_EAST);
            return icon;
        }

        return fileIcon;
    }


    private boolean isSymfonyTranslationDirectory() {
        return ("/" + rootDir).contains("/vendor/") && rootDir.contains("/symfony/") && rootDir.contains("/Resources/translations/");
    }
}
