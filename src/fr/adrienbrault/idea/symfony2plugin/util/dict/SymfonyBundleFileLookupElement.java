package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.icons.AllIcons;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

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

    }

}