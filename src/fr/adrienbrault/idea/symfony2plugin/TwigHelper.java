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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigHelper {

    public static Map<String, TwigFile> getTwigFilesByName(final Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        final Map<String, TwigFile> results = new HashMap<String, TwigFile>();

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(phpIndex).getBundles();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        for (SymfonyBundle bundle : symfonyBundles) {

            final PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(null == views) {
                continue;
            }

            final String prefixName = bundle.getName();

            // dont give use all files:
            // Collection<VirtualFile> twigVirtualFiles = FileTypeIndex.getFiles(TwigFileType.INSTANCE, GlobalSearchScopes.directoryScope(views, true));

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

                    String templateFinalName = prefixName + ":" + templateDirectory + ":" + templateFile;

                    TwigFile twigFile = (TwigFile) PsiManager.getInstance(project).findFile(virtualFile);
                    results.put(templateFinalName, twigFile);

                    return true;
                }
            });

        }

        //@TODO: provide list for prefix match, see twig doc
        final VirtualFile globalDirectory = VfsUtil.findRelativeFile(project.getBaseDir(), "app", "Resources", "views");
        if(globalDirectory == null) {
            return results;
        }

        fileIndex.iterateContentUnderDirectory(globalDirectory, new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile virtualFile) {

                if(!(virtualFile.getFileType() instanceof TwigFileType)) {
                    return true;
                }

                TwigFileType twigFileType = (TwigFileType) virtualFile.getFileType();

                String templatePath = VfsUtil.getRelativePath(virtualFile, globalDirectory, '/');
                String templateDeep = "";
                if (null != templatePath && templatePath.contains("/")) {
                    templateDeep = templatePath.substring(0, templatePath.lastIndexOf("/"));
                }

                TwigFile twigFile = (TwigFile) PsiManager.getInstance(project).findFile(virtualFile);
                if(twigFile != null) {
                    results.put(":" + templateDeep + ":" + twigFile.getName(), twigFile);
                }

                return true;
            }
        });

        return results;
    }


    @Nullable
    public static String getTwigMethodString(@Nullable PsiElement transPsiElement) {
        if (transPsiElement == null) return null;

        ElementPattern<PsiElement> pattern = PlatformPatterns.psiElement(TwigTokenTypes.RBRACE);

        String currentText = transPsiElement.getText();
        for (PsiElement child = transPsiElement.getNextSibling(); child != null; child = child.getNextSibling()) {
            currentText = currentText + child.getText();
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return currentText;
            }
        }

        return null;
    }

    public static ElementPattern<PsiElement> getAutocompletableBlockPattern() {
        return PlatformPatterns
            .psiElement().withParent(
                PlatformPatterns.psiElement(TwigCompositeElementTypes.BLOCK_TAG).withText(
                    PlatformPatterns.string().startsWith("{% block")
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getGoToBlockPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(TwigCompositeElementTypes.BLOCK_TAG).withText(
                    PlatformPatterns.string().startsWith("{% block")
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTransDomainPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(TwigCompositeElementTypes.PRINT_BLOCK).withText(PlatformPatterns.string().contains("trans"))
            )
            .withLanguage(TwigLanguage.INSTANCE);
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

    public static ElementPattern<PsiElement> getTranslationPattern() {
        return
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .beforeLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.FILTER)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("trans")
                )
                .withLanguage(TwigLanguage.INSTANCE);
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
