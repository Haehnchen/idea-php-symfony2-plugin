package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * "Search everywhere" does instance check on NavigationItem, but ChooseByNameContributor use getPresentation :)
 * Moreover "Search everywhere" dont set icons at all
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NavigationItemEx implements NavigationItem, ItemPresentation {

    private final PsiElement psiElement;
    private final String name;
    private final Icon icon;
    private final String locationString;
    private boolean appendBundleLocation = true;

    public NavigationItemEx(PsiElement psiElement, final String name, final Icon icon, final String locationString) {
        this.psiElement = psiElement;
        this.name = name;
        this.icon = icon;
        this.locationString = locationString;
    }

    public NavigationItemEx(PsiElement psiElement, final String name, final Icon icon, final String locationString, boolean appendBundleLocation) {
        this(psiElement, name, icon, locationString);
        this.appendBundleLocation = appendBundleLocation;
    }

    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return this;
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

    @Nullable
    @Override
    public String getPresentableText() {
        return name;
    }

    @Nullable
    @Override
    public String getLocationString() {

        if(!this.appendBundleLocation) {
            return this.locationString;
        }

        PsiFile psiFile = psiElement.getContainingFile();

        if(psiFile == null) {
            return this.locationString;
        }

        String locationPathString = this.locationString;

        String bundleName = psiFile.getVirtualFile().getPath();

        if(bundleName.contains("Bundle")) {
            bundleName = bundleName.substring(0, bundleName.lastIndexOf("Bundle"));
            if(bundleName.length() > 1 && bundleName.contains("/")) {
                return locationPathString + " " + bundleName.substring(bundleName.lastIndexOf("/") + 1, bundleName.length()) + "::" + psiFile.getName();
            }
        }

        return locationPathString + " " + psiFile.getName();
    }

    @Nullable
    @Override
    public Icon getIcon(boolean b) {
        return icon;
    }

}

