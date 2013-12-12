package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class NavigationItemEx implements NavigationItem {

    private PsiElement psiElement;
    private String name;
    private Icon icon;
    private String locationString;

    public NavigationItemEx(PsiElement psiElement, final String name, final Icon icon, final String locationString) {
        this.psiElement = psiElement;
        this.name = name;
        this.icon = icon;
        this.locationString = locationString;
    }

    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return new PresentationData(this.name, this.locationString, this.icon, null);
    }

    @Override
    public void navigate(boolean requestFocus) {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this.psiElement);
        if (descriptor != null) {
            descriptor.navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this.psiElement);
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public String toString() {
        return this.name;
    }
}

