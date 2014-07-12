package fr.adrienbrault.idea.symfony2plugin.asset.provider;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import org.jetbrains.annotations.NotNull;

public class AssetCompletionProvider extends CompletionProvider<CompletionParameters> {

    protected AssetDirectoryReader assetParser;

    public void addCompletions(@NotNull CompletionParameters parameters,
                               ProcessingContext context,
                               @NotNull final CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        this.assetParser.setProject(project);
        for (final AssetFile assetFile : this.assetParser.getAssetFiles()) {
            resultSet.addElement(new AssetLookupElement(assetFile, project));
        }
    }

    public AssetCompletionProvider setAssetParser(AssetDirectoryReader assetParser) {
       this.assetParser = assetParser;
       return this;
    }

}
