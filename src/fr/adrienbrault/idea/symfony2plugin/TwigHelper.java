package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.twig.*;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigHelper {

    public static Map<String, TwigFile> getTwigFilesByName(final Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        final Map<String, TwigFile> results = new HashMap<String, TwigFile>();

        for (final SymfonyBundle bundle : new SymfonyBundleUtil(phpIndex).getBundles()) {

            final PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(null == views) {
                continue;
            }

            // dont give use all files:
            // Collection<VirtualFile> twigVirtualFiles = FileTypeIndex.getFiles(TwigFileType.INSTANCE, GlobalSearchScopes.directoryScope(views, true));

            ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
            fileIndex.iterateContentUnderDirectory(views.getVirtualFile(), new ContentIterator() {
                @Override
                public boolean processFile(final VirtualFile virtualFile) {

                    if(!(virtualFile.getFileType() instanceof TwigFileType)) {
                      return true;
                    }

                    String templatePath = VfsUtil.getRelativePath(virtualFile, views.getVirtualFile(), '/');
                    if(null == templatePath) {
                        return true;
                    }

                    String templateDirectory = null; // xxx:XXX:xxx
                    String templateFile = null; // xxx:xxx:XXX

                    if (templatePath.contains("/")) {
                        int lastDirectorySeparatorIndex = templatePath.lastIndexOf("/");
                        templateDirectory = templatePath.substring(0, lastDirectorySeparatorIndex);
                        templateFile = templatePath.substring(lastDirectorySeparatorIndex + 1);
                    } else {
                        templateDirectory = "";
                        templateFile = templatePath;
                    }

                    String templateFinalName = bundle.getName() + ":" + templateDirectory + ":" + templateFile;

                    TwigFile twigFile = (TwigFile) PsiManager.getInstance(project).findFile(virtualFile);
                    results.put(templateFinalName, twigFile);

                    return true;
                }
            });

        }

        return results;
    }

    public static ElementPattern<PsiElement> getAutocompletableTemplatePattern() {
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigCompositeElementTypes.EMBED_TAG),
                        PlatformPatterns.psiElement(TwigTagWithFileReference.class)
                    )
                )
                .withLanguage(TwigLanguage.INSTANCE),

            // Targetting {{ render(..) }} is tricky right ? :p
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("include")
                )
                .withLanguage(TwigLanguage.INSTANCE)
        );
    }

    public static ElementPattern<PsiElement> getAutocompletableRoutePattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("path"),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("url")
                )
            )
            .withLanguage(TwigLanguage.INSTANCE)
        ;
    }

    public static ElementPattern<PsiElement> getAutocompletableAssetPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("asset")
            )
            .withLanguage(TwigLanguage.INSTANCE)
        ;
    }

    public static ElementPattern<PsiElement> getAutocompletableAssetTag(String tagName) {

        // @TODO: withChild is not working so we are filtering on text
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(PlatformPatterns
                .psiElement(TwigCompositeElementTypes.TAG)
                .withText(PlatformPatterns.string().startsWith("{% " + tagName))
            );
    }

}
