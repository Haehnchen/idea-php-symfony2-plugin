package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.NotNull;

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
                    List<VirtualFile> files = project.getComponent(Symfony2ProjectComponent.class).getAssetFiles();
                    VirtualFile webDirectory = VfsUtil.findRelativeFile(project.getBaseDir(), "web");

                    if (null == webDirectory) {
                        return;
                    }

                    for (final VirtualFile file : files) {
                        resultSet.addElement(new AssetLookupElement(file, webDirectory, project));
                    }
                }
            }
        );
    }

}
