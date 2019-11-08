package fr.adrienbrault.idea.symfonyplugin.asset.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfonyplugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfonyplugin.asset.AssetFile;
import fr.adrienbrault.idea.symfonyplugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfonyplugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetCompletionProvider extends CompletionProvider<CompletionParameters> {
    @NotNull
    final private AssetDirectoryReader assetParser;

    final private boolean includeCustom;

    public AssetCompletionProvider(@NotNull AssetDirectoryReader assetParser, boolean includeCustom) {
        this.assetParser = assetParser;
        this.includeCustom = includeCustom;
    }

    public AssetCompletionProvider(@NotNull AssetDirectoryReader assetParser) {
        this(assetParser, false);
    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull final CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        for (AssetFile assetFile : assetParser.getAssetFiles(project)) {
            resultSet.addElement(new AssetLookupElement(assetFile, project));
        }

        if(includeCustom) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            for (String s : twigPathServiceParser.getNamedAssets().keySet()) {
                resultSet.addElement(LookupElementBuilder.create("@" + s).withIcon(PlatformIcons.FOLDER_ICON).withTypeText("Custom Assets", true));
            }
        }
    }
}
