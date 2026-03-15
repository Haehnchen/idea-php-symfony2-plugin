package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Wraps a block PSI element for the goto-declaration popover when multiple targets exist,
 * enriching each entry with a project-relative file path so the user can distinguish
 * blocks with the same name defined in different templates.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockFakePsiNavigationItem extends GoToSymbolProvider.BaseNavigationItem {
    private final VirtualFile projectDir;
    private final PsiElement myPsiElement;

    public TwigBlockFakePsiNavigationItem(@Nullable VirtualFile projectDir, @NotNull PsiElement psiElement) {
        super(psiElement, "", null);

        this.projectDir = projectDir;
        this.myPsiElement = psiElement;
    }

    @Override
    public Icon getIcon(boolean flags) {
        return myPsiElement.getContainingFile().getIcon(ICON_FLAG_VISIBILITY);
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                return myPsiElement.getText();
            }

            @Override
            public String getLocationString() {
                if (projectDir != null) {
                    String relativePath = VfsUtil.getRelativePath(myPsiElement.getContainingFile().getVirtualFile(), projectDir, '/');
                    if (relativePath != null) {
                        return '(' + relativePath + ')';
                    }
                }


                return '(' + myPsiElement.getContainingFile().getVirtualFile().toString() + ')';
            }

            @Override
            @Nullable
            public Icon getIcon(boolean open) {
                return TwigBlockFakePsiNavigationItem.this.getIcon(ICON_FLAG_VISIBILITY);
            }
        };
    }
}
