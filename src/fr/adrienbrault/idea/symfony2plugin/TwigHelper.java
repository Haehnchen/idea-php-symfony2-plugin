package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigHelper {

    synchronized public static Map<String, PsiFile> getTemplateFilesByName(Project project, boolean useTwig, boolean usePhp) {
        Map<String, PsiFile> results = new HashMap<String, PsiFile>();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        ArrayList<TwigPath> twigPaths = new ArrayList<TwigPath>();
        twigPaths.addAll(getTwigNamespaces(project));

        for (TwigPath twigPath : twigPaths) {
            if(twigPath.isEnabled()) {
                VirtualFile virtualDirectoryFile = twigPath.getDirectory(project);
                if(virtualDirectoryFile != null) {
                    TwigPathContentIterator twigPathContentIterator = new TwigPathContentIterator(project, twigPath).setWithPhp(usePhp).setWithTwig(useTwig);
                    fileIndex.iterateContentUnderDirectory(virtualDirectoryFile, twigPathContentIterator);
                    results.putAll(twigPathContentIterator.getResults());
                }
            }

        }

        return results;
    }

    synchronized public static Map<String, TwigFile> getTwigFilesByName(Project project) {
        Map<String, TwigFile> results = new HashMap<String, TwigFile>();
        for(Map.Entry<String, PsiFile> entry: getTemplateFilesByName(project, true, true).entrySet()) {
            if(entry.getValue() instanceof TwigFile) {
                results.put(entry.getKey(), (TwigFile) entry.getValue());
            }
        }

        return results;
    }

    synchronized public static Map<String, PsiFile> getTemplateFilesByName(Project project) {
        return getTemplateFilesByName(project, true, true);
    }

    @Nullable
    public static TwigNamespaceSetting findManagedTwigNamespace(Project project, TwigPath twigPath) {

        ArrayList<TwigNamespaceSetting> twigNamespaces = (ArrayList<TwigNamespaceSetting>) Settings.getInstance(project).twigNamespaces;
        if(twigNamespaces == null) {
            return null;
        }

        for(TwigNamespaceSetting twigNamespace: twigNamespaces) {
           if(twigNamespace.equals(project, twigPath)) {
                return twigNamespace;
           }
        }

        return null;
    }

    public static PsiElement[] getTemplatePsiElements(Project project, String templateName) {

        // both are valid names first is internal completion
        // @TODO: provide setting for that
        // BarBundle:Foo:steps/step_finish.html.twig
        // BarBundle:Foo/steps:step_finish.html.twig

        if(templateName.matches("^.*?:.*?:.*?/.*?$")) {
            int lastDoublePoint = templateName.lastIndexOf(":");
            String subFolder = templateName.substring(lastDoublePoint + 1, templateName.lastIndexOf("/"));
            String file = templateName.substring(templateName.lastIndexOf("/") + 1);
            templateName = templateName.substring(0, lastDoublePoint) + "/" + subFolder + ":" + file;
        }

        Map<String, PsiFile> twigFiles = TwigHelper.getTemplateFilesByName(project);
        if(!twigFiles.containsKey(templateName)) {
            return new PsiElement[0];
        }

        return new PsiElement[] {twigFiles.get(templateName)};
    }

    synchronized public static ArrayList<TwigPath> getTwigNamespaces(Project project) {
       return getTwigNamespaces(project, true);
    }

    synchronized public static ArrayList<TwigPath> getTwigNamespaces(Project project, boolean includeSettings) {
        ArrayList<TwigPath> twigPaths = new ArrayList<TwigPath>();
        PhpIndex phpIndex = PhpIndex.getInstance(project);

        TwigPathServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigPathServiceParser.class);
        twigPaths.addAll(twigPathServiceParser.getTwigPathIndex().getTwigPaths());

        String appDirectoryName = Settings.getInstance(project).directoryToApp + "/Resources/views";
        VirtualFile globalDirectory = VfsUtil.findRelativeFile(project.getBaseDir(), appDirectoryName.split("/"));
        if(globalDirectory != null) {
            twigPaths.add(new TwigPath(globalDirectory.getPath(), TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.BUNDLE));
        }

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(phpIndex).getBundles();
        for (SymfonyBundle bundle : symfonyBundles) {
            PsiDirectory views = bundle.getSubDirectory("Resources", "views");
            if(views != null) {
                twigPaths.add(new TwigPath(views.getVirtualFile().getPath(), bundle.getName(), TwigPathIndex.NamespaceType.BUNDLE));
            }
        }

        for(TwigPath twigPath: twigPaths) {
            TwigNamespaceSetting twigNamespaceSetting = findManagedTwigNamespace(project, twigPath);
            if(twigNamespaceSetting != null) {
                twigPath.setEnabled(false);
            }
        }

        if(!includeSettings) {
            return twigPaths;
        }

        ArrayList<TwigNamespaceSetting> twigNamespaceSettings = (ArrayList<TwigNamespaceSetting>) Settings.getInstance(project).twigNamespaces;
        if(twigNamespaceSettings != null) {
            for(TwigNamespaceSetting twigNamespaceSetting: twigNamespaceSettings) {
                if(twigNamespaceSetting.isCustom()) {
                    twigPaths.add(new TwigPath(twigNamespaceSetting.getPath(), twigNamespaceSetting.getNamespace(), twigNamespaceSetting.getNamespaceType(), true).setEnabled(twigNamespaceSetting.isEnabled()));

                }
            }
        }

        return twigPaths;
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
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)
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
    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern() {
        return  PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)).withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getBlockTagPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.BLOCK_TAG)
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

    public static ElementPattern<PsiElement> getTransDefaultDomain() {

        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .withParent(
                    PlatformPatterns.psiElement(TwigTagWithFileReference.class)
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("trans_default_domain")
                ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(TwigTagWithFileReference.class)
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("trans_default_domain")
                ).withLanguage(TwigLanguage.INSTANCE)
        );
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
                .psiElement(TwigElementTypes.PRINT_BLOCK)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getPathAfterLeafPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL)
                )
            )
            .withParent(PlatformPatterns.psiElement().withText(PlatformPatterns.string().contains("path")))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTypeCompletionPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeaf(
                PlatformPatterns.psiElement(TwigTokenTypes.DOT)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static PsiElementPattern.Capture<PsiComment> getTwigTypeDocBlock() {
        return PlatformPatterns
            .psiComment().withText(PlatformPatterns.string().matches(TwigTypeResolveUtil.DOC_PATTERN))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getRoutePattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText("path")
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
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

    public static ElementPattern<PsiElement> getTranslationPattern(String... type) {
        return
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .beforeLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.FILTER).beforeLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(PsiWhiteSpace.class)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                            PlatformPatterns.string().oneOf(type)
                        )
                    )
                )
                .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getAutocompletableFilterPattern() {
        return
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .afterLeaf(
                    PlatformPatterns.psiElement(TwigTokenTypes.FILTER)
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
                .psiElement(TwigCompositeElement.class)
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
        // first: {% from '<xxx>' import foo, <|>  %}
        // second: {% from '<xxx>' import <|>  %}
        // and not: {% from '<xxx>' import foo as <|>  %}
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(PlatformPatterns.psiElement(TwigElementTypes.IMPORT_TAG))
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                    PlatformPatterns.psiElement(TwigTokenTypes.AS_KEYWORD),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IMPORT_KEYWORD)
            ).andNot(PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.AS_KEYWORD)
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getForTagVariable() {

        // {% for key, user in users %}
        // {% for user in users %}
        // {% for user in users|slice(0, 10) %}
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IN)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getForTagInVariable() {

        // {% for key, user in users %}
        // {% for user in users %}
        // {% for user in users|slice(0, 10) %}
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IN)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ArrayList<VirtualFile> resolveAssetsFiles(Project project, String templateName, String... fileTypes) {


        ArrayList<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();

        // {% javascripts '@SampleBundle/Resources/public/js/*' %}
        // {% javascripts 'assets/js/*' %}
        // {% javascripts 'assets/js/*.js' %}
        Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(templateName);
        if (!matcher.find()) {

            for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension(fileTypes).setIncludeBundleDir(true).setProject(project).getAssetFiles()) {
                if(assetFile.toString().equals(templateName)) {
                    virtualFiles.add(assetFile.getFile());
                }
            }

            return virtualFiles;
        }

        String pathName = matcher.group(1);
        String fileExtension = matcher.group(2).length() > 0 ? matcher.group(2) : null;

        for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension(fileTypes).setIncludeBundleDir(true).setProject(project).getAssetFiles()) {
            if(fileExtension == null && assetFile.toString().matches(Pattern.quote(pathName) + "(?!.*[/\\\\]).*\\.\\w+")) {
                virtualFiles.add(assetFile.getFile());
            } else if(fileExtension != null && assetFile.toString().matches(Pattern.quote(pathName) + "(?!.*[/\\\\]).*" + Pattern.quote(fileExtension))) {
                virtualFiles.add(assetFile.getFile());
            }
        }

        return virtualFiles;
    }

    /**
     * twig lexer just giving use a flat psi list for a block. we need custom stuff to resolve this
     * path('route', {'<parameter>':
     * path('route', {'<parameter>': '', '<parameter2>': ''
     */
    @Nullable
    public static String getMatchingRouteNameOnParameter(PsiElement startPsiElement) {
        String prevText = PhpElementsUtil.getPrevSiblingAsTextUntil(startPsiElement, TwigHelper.getRoutePattern(), true);

        String regex = "^path\\(([\"|'])([\\w-]+)\\1[\\s]*,[\\s]*\\{[\\s]*.*['|\"]$";
        Matcher matcher = Pattern.compile(regex).matcher(prevText.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(2);
        }

        return null;
    }

}
