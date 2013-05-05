package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class AssetLookupElement extends LookupElement {
    protected AssetFile assetFile;
    protected Project project;

    public AssetLookupElement(AssetFile assetfile, Project project) {
        this.assetFile = assetfile;
        this.project = project;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return assetFile.toString();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {

        String typeText = "Web";
        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            typeText = "Bundle";
        }

        presentation.setItemText(getLookupString());
        presentation.setTypeText(typeText);
        presentation.setIcon(IconUtil.getIcon(assetFile.getFile(), 0, project));

    }
}
