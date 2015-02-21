package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.include.FileIncludeIndex;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.path.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigHelper {

    public static String[] CSS_FILES_EXTENSIONS = new String[] { "css", "less", "sass", "scss" };
    public static String[] JS_FILES_EXTENSIONS = new String[] { "js", "dart", "coffee" };
    public static String[] IMG_FILES_EXTENSIONS = new String[] { "png", "jpg", "jpeg", "gif" };

    public static String TEMPLATE_ANNOTATION_CLASS = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template";

    public static Map<String, VirtualFile> getTemplateFilesByName(Project project, boolean useTwig, boolean usePhp) {

        Map<String, VirtualFile> results = new HashMap<String, VirtualFile>();

        List<TwigPath> twigPaths = new ArrayList<TwigPath>();
        twigPaths.addAll(getTwigNamespaces(project));

        if(twigPaths.size() == 0) {
            return results;
        }

        for (TwigPath twigPath : twigPaths) {
            if(twigPath.isEnabled()) {
                VirtualFile virtualDirectoryFile = twigPath.getDirectory(project);
                if(virtualDirectoryFile != null) {

                    final TwigPathContentIterator twigPathContentIterator = new TwigPathContentIterator(project, twigPath).setWithPhp(usePhp).setWithTwig(useTwig);
                    VfsUtil.visitChildrenRecursively(virtualDirectoryFile, new VirtualFileVisitor() {
                        @Override
                        public boolean visitFile(@NotNull VirtualFile virtualFile) {
                            twigPathContentIterator.processFile(virtualFile);
                            return super.visitFile(virtualFile);
                        }
                    });

                    results.putAll(twigPathContentIterator.getResults());
                }
            }

        }

        return results;
    }

    public static Map<String, VirtualFile> getTwigFilesByName(Project project) {
        return getTemplateFilesByName(project, true, false);
    }

    public static Map<String, VirtualFile> getTemplateFilesByName(Project project) {
        return getTemplateFilesByName(project, true, true);
    }

    @Nullable
    public static TwigNamespaceSetting findManagedTwigNamespace(Project project, TwigPath twigPath) {

        List<TwigNamespaceSetting> twigNamespaces = Settings.getInstance(project).twigNamespaces;
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

    @Nullable
    public static PsiFile getTemplateFileByName(Project project, String templateName) {

        PsiFile[] templatePsiElements = TwigHelper.getTemplatePsiElements(project, templateName);
        if(templatePsiElements.length > 0) {
            return templatePsiElements[0];
        }

        return null;
    }

    /**
     * both are valid names first is internal completion
     * BarBundle:Foo:steps/step_finish.html.twig
     * BarBundle:Foo/steps:step_finish.html.twig
     *
     * todo: provide setting for that
     */
    public static String normalizeTemplateName(String templateName) {

        // force linux path style
        templateName = templateName.replace("\\", "/");

        if(templateName.startsWith("@") || !templateName.matches("^.*?:.*?:.*?/.*?$")) {
            return templateName;
        }

        templateName = templateName.replace(":", "/");

        int firstDoublePoint = templateName.indexOf("/");
        int lastDoublePoint = templateName.lastIndexOf("/");

        String bundle = templateName.substring(0, templateName.indexOf("/"));
        String subFolder = templateName.substring(firstDoublePoint, lastDoublePoint);
        String file = templateName.substring(templateName.lastIndexOf("/") + 1);

        return String.format("%s:%s:%s", bundle, StringUtils.strip(subFolder, "/"), file);

    }

    /**
     * Find file in a twig path collection
     *
     * @param project current project
     * @param templateName path known, should not be normalized
     * @return target files
     */
    public static PsiFile[] getTemplatePsiElements(Project project, String templateName) {


        String normalizedTemplateName = normalizeTemplateName(templateName);

        Collection<PsiFile> psiFiles = new HashSet<PsiFile>();

        for (TwigPath twigPath : getTwigNamespaces(project)) {

            if(!twigPath.isEnabled()) {
                continue;
            }

            if(normalizedTemplateName.startsWith("@")) {
                // @Namespace/base.html.twig
                // @Namespace/folder/base.html.twig
                if(normalizedTemplateName.length() > 1 && twigPath.getNamespaceType() != TwigPathIndex.NamespaceType.BUNDLE) {
                    int i = normalizedTemplateName.indexOf("/");
                    if(i > 0) {
                        String templateNs = normalizedTemplateName.substring(1, i);
                        if(twigPath.getNamespace().equals(templateNs)) {
                            addFileInsideTwigPath(project, normalizedTemplateName.substring(i + 1), psiFiles, twigPath);
                        }
                    }
                }
            } else if(normalizedTemplateName.startsWith(":")) {
                // ::base.html.twig
                // :Foo:base.html.twig
                if(normalizedTemplateName.length() > 1 && twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE && twigPath.isGlobalNamespace()) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, psiFiles, twigPath);
                }
            } else {
                // FooBundle::base.html.twig
                // FooBundle:Bar:base.html.twig
                if(twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE) {
                    int i = normalizedTemplateName.indexOf(":");
                    if(i > 0) {
                        String templateNs = normalizedTemplateName.substring(0, i);
                        if(twigPath.getNamespace().equals(templateNs)) {
                            String templatePath = StringUtils.strip(normalizedTemplateName.substring(i + 1).replace(":", "/").replace("//", "/"), "/");
                            addFileInsideTwigPath(project, templatePath, psiFiles, twigPath);
                        }

                    }
                }

                // form_div_layout.html.twig
                if(twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.ADD_PATH) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, psiFiles, twigPath);
                }

                // Bundle overwrite:
                // FooBundle:index.html -> app/views/FooBundle:index.html
                if(twigPath.isGlobalNamespace() && !normalizedTemplateName.startsWith(":") && !normalizedTemplateName.startsWith("@")) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/").replace("//", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, psiFiles, twigPath);
                }

            }

        }

        return psiFiles.toArray(new PsiFile[psiFiles.size()]);

    }

    private static void addFileInsideTwigPath(Project project, String templatePath, Collection<PsiFile> psiFiles, TwigPath twigPath) {
        String[] split = templatePath.split("/");
        VirtualFile virtualFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), split);
        if(virtualFile != null) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                psiFiles.add(psiFile);
            }
        }
    }

    public static List<TwigPath> getTwigNamespaces(@NotNull Project project) {
       return getTwigNamespaces(project, true);
    }

    public static List<TwigPath> getTwigNamespaces(@NotNull Project project, boolean includeSettings) {
        List<TwigPath> twigPaths = new ArrayList<TwigPath>();
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

        List<TwigNamespaceSetting> twigNamespaceSettings = Settings.getInstance(project).twigNamespaces;
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
    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern(String... functionName) {
        //noinspection unchecked
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
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% embed "vertical_boxes_skeleton.twig" %}
     */
    public static ElementPattern<PsiElement> getEmbedPattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.EMBED_TAG)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("embed")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern() {
        return  PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)).withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ form(foo) }}, {{ foo }}
     * NOT: {{ foo.bar }}, {{ 'foo.bar' }}
     */
    public static ElementPattern<PsiElement> getCompletablePattern() {
        //noinspection unchecked
        return  PlatformPatterns.psiElement()
            .andNot(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOT)),
                    PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE)),
                    PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE))
                )
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class)
                ),
                PlatformPatterns.psiElement()
            )
            .withParent(PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getBlockTagPattern() {
        //noinspection unchecked
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
        //noinspection unchecked
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
     * match ", 'dddd')" on ending
     */
    public static ElementPattern<PsiElement> getTransDomainPattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
            )
            .withParent(PlatformPatterns
                .psiElement(TwigElementTypes.PRINT_BLOCK)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getPathAfterLeafPattern() {
        //noinspection unchecked
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

    public static ElementPattern<PsiElement> getParentFunctionPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withText("parent")
            .beforeLeaf(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
            )
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

    public static ElementPattern<PsiComment> getTwigTypeDocBlock() {
        return PlatformPatterns.or(
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(TwigTypeResolveUtil.DOC_PATTERN)).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(TwigTypeResolveUtil.DOC_PATTERN_2)).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    public static PsiElementPattern.Capture<PsiComment> getTwigDocBlockMatchPattern(String pattern) {
        return PlatformPatterns
            .psiComment().withText(PlatformPatterns.string().matches(pattern))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static PsiElementPattern.Capture<PsiElement> getFormThemeFileTag() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(PlatformPatterns.psiElement().withText(PlatformPatterns.string().matches("\\{%\\s+form_theme.*")))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getRoutePattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText("path")
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getAutocompletableRoutePattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
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
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("asset")
            )
            .withLanguage(TwigLanguage.INSTANCE)
        ;
    }

    public static ElementPattern<PsiElement> getTranslationPattern(String... type) {
        //noinspection unchecked
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
        //noinspection unchecked
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
        return getTemplateFileReferenceTagPattern("extends", "from", "include", "use", "import", "embed");
    }

    public static ElementPattern<PsiElement> getTemplateFileReferenceTagPattern(String... tagNames) {

        // {% include '<xxx>' with {'foo' : bar, 'bar' : 'foo'} %}

        //noinspection unchecked
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
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf(tagNames))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTemplateImportFileReferenceTagPattern() {

        // first: {% from '<xxx>' import foo, <|>  %}
        // second: {% from '<xxx>' import <|>  %}
        // and not: {% from '<xxx>' import foo as <|>  %}

        //noinspection unchecked
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

    public static ElementPattern<PsiElement> getForTagVariablePattern() {
        // {% for "user"  %}

        //noinspection unchecked
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

    public static ElementPattern<PsiElement> getForTagInVariablePattern() {

        // {% for key, user in "users" %}
        // {% for user in "users" %}
        // {% for user in "users"|slice(0, 10) %}

        //noinspection unchecked
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

    public static ElementPattern<PsiElement> getIfVariablePattern() {

        // {% if "var" %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(
                    PlatformPatterns.string().oneOfIgnoreCase("if")
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getIfConditionVariablePattern() {

        // {% if var < "var1" %}
        // {% if var == "var1" %}
        // and so on

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LE),
                    PlatformPatterns.psiElement(TwigTokenTypes.LT),
                    PlatformPatterns.psiElement(TwigTokenTypes.GE),
                    PlatformPatterns.psiElement(TwigTokenTypes.GT),
                    PlatformPatterns.psiElement(TwigTokenTypes.EQ_EQ),
                    PlatformPatterns.psiElement(TwigTokenTypes.NOT_EQ)
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTwigMacroNamePattern() {

        // {% macro <foo>(user) %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(PlatformPatterns.psiElement(
                TwigElementTypes.MACRO_TAG
            ))
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("macro")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTwigTagUseNamePattern() {

        // {% use '<foo>' %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(PlatformPatterns.psiElement(
                TwigElementTypes.TAG
            ))
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("use")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTwigMacroNameKnownPattern(String macroName) {

        // {% macro <foo>(user) %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText(macroName)
            .withParent(PlatformPatterns.psiElement(
                TwigElementTypes.MACRO_TAG
            ))
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("macro")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getSetVariablePattern() {

        // {% set count1 = "var" %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.EQ)
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.SET_TAG)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% include 'foo.html.twig' {'foo': 'foo'} only %}
     */
    public static ElementPattern<PsiElement> getIncludeOnlyPattern() {

        // {% set count1 = "var" %}

        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText("only")
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_END)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getVariableTypePattern() {
        //noinspection unchecked
        return PlatformPatterns.or(
            TwigHelper.getForTagInVariablePattern(),
            TwigHelper.getIfVariablePattern(),
            TwigHelper.getIfConditionVariablePattern(),
            TwigHelper.getSetVariablePattern()
        );
    }

    public static List<VirtualFile> resolveAssetsFiles(Project project, String templateName, String... fileTypes) {


        List<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();

        // {% javascripts [...] @jquery_js2'%}
        if(templateName.startsWith("@") && templateName.length() > 1) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            String assetName = templateName.substring(1);
            if(twigPathServiceParser.getNamedAssets().containsKey(assetName)) {
                for (String s : twigPathServiceParser.getNamedAssets().get(assetName)) {
                    VirtualFile fileByURL = VfsUtil.findFileByIoFile(new File(s), false);
                    if(fileByURL != null) {
                        virtualFiles.add(fileByURL);
                    }
                }
            }

        }

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

    public static Set<String> getTwigMacroSet(Project project) {
        SymfonyProcessors.CollectProjectUniqueKeys ymlProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeys(project, TwigMacroFunctionStubIndex.KEY);
        FileBasedIndexImpl.getInstance().processAllKeys(TwigMacroFunctionStubIndex.KEY, ymlProjectProcessor, project);
        return ymlProjectProcessor.getResult();
    }

    public static Collection<PsiElement> getTwigMacroTargets(final Project project, final String name) {

        final Collection<PsiElement> targets = new ArrayList<PsiElement>();

        FileBasedIndexImpl.getInstance().getFilesWithKey(TwigMacroFunctionStubIndex.KEY, new HashSet<String>(Arrays.asList(name)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {

                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null) {
                    PsiTreeUtil.processElements(psiFile, new PsiElementProcessor() {
                        public boolean execute(@NotNull PsiElement psiElement) {

                            if(getTwigMacroNameKnownPattern(name).accepts(psiElement)) {
                                targets.add(psiElement);
                            }

                            return true;

                        }
                    });
                }

                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        return targets;
    }

    public static Collection<LookupElement> getTwigLookupElements(Project project) {
        VirtualFile baseDir = project.getBaseDir();

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for (Map.Entry<String, VirtualFile> entry : TwigHelper.getTwigFilesByName(project).entrySet()) {
            lookupElements.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue(), baseDir)
            );
        }

        return lookupElements;
    }

    public static Collection<LookupElement> getAllTemplateLookupElements(Project project) {
        VirtualFile baseDir = project.getBaseDir();

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for (Map.Entry<String, VirtualFile> entry : TwigHelper.getTemplateFilesByName(project).entrySet()) {
            lookupElements.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue(), baseDir)
            );
        }

        return lookupElements;
    }

}
