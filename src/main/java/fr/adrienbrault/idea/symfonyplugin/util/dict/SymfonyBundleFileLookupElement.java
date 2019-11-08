package fr.adrienbrault.idea.symfonyplugin.util.dict;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleFileLookupElement extends LookupElement {

    private BundleFile bundleFile;
    private InsertHandler<LookupElement> insertHandler = null;

    public SymfonyBundleFileLookupElement(BundleFile bundleFile) {
        this.bundleFile = bundleFile;
    }

    public SymfonyBundleFileLookupElement(BundleFile bundleFile, InsertHandler<LookupElement> insertHandler) {
        this(bundleFile);
        this.insertHandler = insertHandler;
    }

    @NotNull
    @Override
    public String getLookupString() {
        String shortcutName = this.bundleFile.getShortcutPath();
        if(shortcutName == null) {
            return "";
        }

        // we strip any control char, so only use the pathname
        if(shortcutName.startsWith("@")) {
            shortcutName = shortcutName.substring(1);
        }

        return shortcutName;
    }

    @Override
    public void handleInsert(InsertionContext context) {

        if(this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
            return;
        }

        super.handleInsert(context);
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(this.bundleFile.getSymfonyBundle().getName());
        presentation.setTypeGrayed(true);
        presentation.setIcon(IconUtil.getIcon(this.bundleFile.getVirtualFile(), 0, this.bundleFile.getProject()));

    }

}