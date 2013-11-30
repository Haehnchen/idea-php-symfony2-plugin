package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.util.IconUtil;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetEnum;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;
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

        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            return assetFile.toString().substring(1);
        }

        return assetFile.toString();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            ResourceFileInsertHandler.getInstance().handleInsert(context, this);
        }
        super.handleInsert(context);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {

        String typeText = "Web";
        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            typeText = "Bundle";
        }

        presentation.setItemText(assetFile.toString());
        presentation.setTypeText(typeText);
        presentation.setIcon(IconUtil.getIcon(assetFile.getFile(), 0, project));

    }
}
