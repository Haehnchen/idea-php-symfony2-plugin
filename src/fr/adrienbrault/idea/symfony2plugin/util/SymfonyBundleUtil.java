package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleUtil {

    protected PhpIndex phpIndex;
    protected HashMap<String, SymfonyBundle> symfonyBundles;

    public SymfonyBundleUtil(PhpIndex phpIndex) {
        this.phpIndex = phpIndex;
        this.loadBundles();
    }

    public SymfonyBundleUtil(Project project) {
        this(PhpIndex.getInstance(project));
    }

    protected void loadBundles() {

        this.symfonyBundles = new HashMap<>();
        Collection<PhpClass> phpClasses = this.phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        for (PhpClass phpClass : phpClasses) {
            this.symfonyBundles.put(phpClass.getName(), new SymfonyBundle(phpClass));
        }

    }

    public Collection<SymfonyBundle> getBundles() {
        return this.symfonyBundles.values();
    }

    public Map<String, SymfonyBundle> getParentBundles() {

        Map<String, SymfonyBundle> bundles = new HashMap<>();

        for (Map.Entry<String, SymfonyBundle> entry : this.symfonyBundles.entrySet()) {
            if(entry.getValue().getParentBundleName() != null) {
                bundles.put(entry.getKey(), entry.getValue());
            }
        }

        return bundles;
    }

    @Nullable
    public SymfonyBundle getBundle(String bundleName) {
        return this.symfonyBundles.get(bundleName);
    }

    public boolean bundleExists(String bundleName) {
        return this.symfonyBundles.get(bundleName) != null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(String bundleShortcutName) {

        if(!bundleShortcutName.startsWith("@")) {
           return null;
        }

        int stripedBundlePos = bundleShortcutName.indexOf("/");
        if(stripedBundlePos == -1) {
            return null;
        }

        String bundleName = bundleShortcutName.substring(1, stripedBundlePos);
        for(SymfonyBundle bundle : this.getBundles()) {
            if(bundle.getName().equals(bundleName)) {
                return bundle;
            }
        }

        return null;
    }


    @Nullable
    public SymfonyBundle getContainingBundle(PhpClass phpClass) {

        for(SymfonyBundle bundle : this.getBundles()) {
            if(bundle.isInBundle(phpClass)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(PsiFile psiFile) {

        for(SymfonyBundle bundle : this.getBundles()) {
            if(bundle.isInBundle(psiFile)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(@NotNull VirtualFile virtualFile) {

        for(SymfonyBundle bundle : this.getBundles()) {
            if(bundle.isInBundle(virtualFile)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(PsiDirectory directory) {

        for(SymfonyBundle bundle : this.getBundles()) {
            if(bundle.isInBundle(directory.getVirtualFile())) {
                return bundle;
            }
        }

        return null;
    }

}
