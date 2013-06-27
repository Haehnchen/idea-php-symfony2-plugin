package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.twig.*;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathContentIterator;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigHelper {

    public static Map<String, TwigFile> getTwigFilesByName(Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        final Map<String, TwigFile> results = new HashMap<String, TwigFile>();

        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        ArrayList<String> uniqueNamespaceList = new ArrayList<String>();
        ArrayList<TwigPath> twigPaths = new ArrayList<TwigPath>();

        // unique list for bundle fallback
        TwigPathServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigPathServiceParser.class);
        for (TwigPath twigPath : twigPathServiceParser.getTwigPathIndex().getTwigPaths()) {
            uniqueNamespaceList.add(twigPath.getNamespace());
        }

        twigPaths.addAll(twigPathServiceParser.getTwigPathIndex().getTwigPaths());

        // provide bundle callback for symfony < 2.2, which dont have addPath in container file
        // it looks like not all bundle namespaces get registered in addPath, so add them every time
        getTwigFilesByNameBundleFallback(project, phpIndex, uniqueNamespaceList, twigPaths);

        for (TwigPath twigPath : twigPaths) {
            VirtualFile virtualDirectoryFile = twigPath.getDirectory();
            if(virtualDirectoryFile != null) {
                TwigPathContentIterator twigPathContentIterator = new TwigPathContentIterator(project, twigPath);
                fileIndex.iterateContentUnderDirectory(virtualDirectoryFile, twigPathContentIterator);
                results.putAll(twigPathContentIterator.getResults());
            }

        }


        return results;

    }

    private static void getTwigFilesByNameBundleFallback(Project project, PhpIndex phpIndex, ArrayList<String> uniqueNamespaceList, ArrayList<TwigPath> twigPaths) {
        VirtualFile globalDirectory = VfsUtil.findRelativeFile(project.getBaseDir(), "app", "Resources", "views");
        if(globalDirectory != null) {
            twigPaths.add(new TwigPath(globalDirectory.getPath(), TwigPathIndex.MAIN, true));
        }

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(phpIndex).getBundles();
        for (SymfonyBundle bundle : symfonyBundles) {
            PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(views != null && !uniqueNamespaceList.contains(bundle.getName())) {
                twigPaths.add(new TwigPath(views.getVirtualFile().getPath(), bundle.getName(), true));
            }
        }
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

    /**
     * Check for {{ include('|')  }}
     *
     * @param functionName twig function name
     */
    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern(String functionName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(TwigCompositeElementTypes.PRINT_BLOCK)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(functionName)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getBlockTagPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(TwigCompositeElementTypes.BLOCK_TAG)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("block")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * match 'dddd') on ending
     */
    public static ElementPattern<PsiElement> getTransDomainPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)
            )
            .withParent(PlatformPatterns
                .psiElement(TwigCompositeElementTypes.PRINT_BLOCK)
            )
            .withLanguage(TwigLanguage.INSTANCE);
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

        // pattern to match '..foo.css' but not match eg ='...'
        //
        // {% stylesheets filter='cssrewrite'
        //  'assets/css/foo.css'
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class)
                )
            .withParent(PlatformPatterns
                .psiElement(TwigCompositeElementTypes.TAG)
                .withText(PlatformPatterns.string().startsWith("{% " + tagName))
            );
    }
    public static ElementPattern<PsiElement> getTemplateFileReferenceTagPattern() {

        // {% include '<xxx>' with {'foo' : bar, 'bar' : 'foo'} %}
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("extends", "from", "include", "use", "import", "embed"))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTemplateImportFileReferenceTagPattern() {


        // @TODO: first pattern need some more filter; also 'from' tag would be nice
        // first: {% from '<xxx>' import foo, <|>  %}
        // second: {% from '<xxx>' import <|>  %}
        return
            PlatformPatterns.or(
                PlatformPatterns
                    .psiElement(TwigTokenTypes.IDENTIFIER)
                    .withParent(TwigTagWithFileReference.class)
                    .afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withoutText("import"),
                            PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                            PlatformPatterns.string().oneOf("import")
                        )
                    )
                    .withLanguage(TwigLanguage.INSTANCE),
                PlatformPatterns
                    .psiElement(TwigTokenTypes.IDENTIFIER)
                    .withParent(TwigTagWithFileReference.class)
                    .afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                            PlatformPatterns.string().oneOf("import")
                        )
                    )
                    .withLanguage(TwigLanguage.INSTANCE)
            );
    }


}
