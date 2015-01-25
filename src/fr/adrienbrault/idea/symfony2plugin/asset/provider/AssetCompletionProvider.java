package fr.adrienbrault.idea.symfony2plugin.asset.provider;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

public class AssetCompletionProvider extends CompletionProvider<CompletionParameters> {

    protected AssetDirectoryReader assetParser;
    protected boolean includeCustom = false;

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

        if(includeCustom) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            for (String s : twigPathServiceParser.getNamedAssets().keySet()) {
                resultSet.addElement(LookupElementBuilder.create("@" + s).withIcon(PlatformIcons.FOLDER_ICON).withTypeText("Custom Assets", true));
            }
        }

    }

    public AssetCompletionProvider setAssetParser(AssetDirectoryReader assetParser) {
       this.assetParser = assetParser;
       return this;
    }

    public AssetCompletionProvider setIncludeCustom(boolean includeCustom) {
        this.includeCustom = includeCustom;
        return this;
    }

}
