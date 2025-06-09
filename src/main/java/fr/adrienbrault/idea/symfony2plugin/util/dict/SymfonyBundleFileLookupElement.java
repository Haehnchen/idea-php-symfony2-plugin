package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleFileLookupElement extends LookupElement {
    private final @NotNull String bundleName;
    private final BundleFile bundleFile;
    private InsertHandler<LookupElement> insertHandler = null;
    private final @Nullable String shortcutName;

    public SymfonyBundleFileLookupElement(BundleFile bundleFile) {
        this.bundleFile = bundleFile;
        this.shortcutName = bundleFile.getShortcutPath();
        this.bundleName = bundleFile.getSymfonyBundle().getName();
    }

    public SymfonyBundleFileLookupElement(BundleFile bundleFile, InsertHandler<LookupElement> insertHandler) {
        this(bundleFile);
        this.insertHandler = insertHandler;
    }

    @NotNull
    @Override
    public String getLookupString() {
        if (shortcutName == null) {
            return "";
        }

        // we strip any control char, so only use the pathname
        if (shortcutName.startsWith("@")) {
            return shortcutName.substring(1);
        }

        return shortcutName;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
            return;
        }

        super.handleInsert(context);
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(bundleName);
        presentation.setTypeGrayed(true);
        presentation.setIcon(IconUtil.getIcon(this.bundleFile.getVirtualFile(), 0, this.bundleFile.getProject()));
    }
}