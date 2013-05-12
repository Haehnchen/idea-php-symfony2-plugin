package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;

public class SymfonyBundleUtil {

    protected PhpIndex phpIndex;
    protected HashMap<String, SymfonyBundle> symfonyBundles;

    public SymfonyBundleUtil(PhpIndex phpIndex) {
        this.phpIndex = phpIndex;
        this.loadBundles();
    }

    protected void loadBundles() {

        this.symfonyBundles = new HashMap<String, SymfonyBundle>();
        Collection<PhpClass> phpClasses = this.phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        for (PhpClass phpClass : phpClasses) {
            this.symfonyBundles.put(phpClass.getName(), new SymfonyBundle(phpClass));
        }

    }

    public Collection<SymfonyBundle> getBundles() {
        return this.symfonyBundles.values();
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

}
