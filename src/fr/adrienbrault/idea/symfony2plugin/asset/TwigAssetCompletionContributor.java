package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigAssetCompletionContributor extends CompletionContributor {

    public TwigAssetCompletionContributor() {
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableAssetPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull final CompletionResultSet resultSet) {
                    Project project = parameters.getPosition().getProject();
                    List<AssetFile> files = project.getComponent(Symfony2ProjectComponent.class).getAssetFiles();

                    for (final AssetFile assetFile : files) {
                        resultSet.addElement(new AssetLookupElement(assetFile, project));
                    }
                }
            }
        );
    }

}
