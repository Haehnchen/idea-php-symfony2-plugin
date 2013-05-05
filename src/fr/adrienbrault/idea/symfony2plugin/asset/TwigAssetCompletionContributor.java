package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.completion.*;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.provider.AssetCompletionProvider;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigAssetCompletionContributor extends CompletionContributor {

    public TwigAssetCompletionContributor() {

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetPattern(), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader()
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("stylesheets"), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader().setFilterExtension("css").setIncludeBundleDir(true)
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("javascripts"), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader().setFilterExtension("js").setIncludeBundleDir(true)
        ));

    }


}
