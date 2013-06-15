package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleFileLookupElement extends LookupElement {

    private BundleFile bundleFile;

    public SymfonyBundleFileLookupElement(BundleFile bundleFile) {
        this.bundleFile = bundleFile;
    }

    @NotNull
    @Override
    public String getLookupString() {
        String shortcutName = this.bundleFile.getShortcutPath();
        return shortcutName != null ? shortcutName : "";
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(this.bundleFile.getSymfonyBundle().getName());
        presentation.setTypeGrayed(true);
        presentation.setIcon(IconUtil.getIcon(this.bundleFile.getVirtualFile(), 0, this.bundleFile.getProject()));

    }

}