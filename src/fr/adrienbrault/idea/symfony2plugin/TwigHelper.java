package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateFileMap;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.path.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigHelper {

    private static final ExtensionPointName<TwigNamespaceExtension> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension"
    );

    public static String[] CSS_FILES_EXTENSIONS = new String[] { "css", "less", "sass", "scss" };
    public static String[] JS_FILES_EXTENSIONS = new String[] { "js", "dart", "coffee" };
    public static String[] IMG_FILES_EXTENSIONS = new String[] { "png", "jpg", "jpeg", "gif" };

    public static String TEMPLATE_ANNOTATION_CLASS = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template";

    private static final Key<CachedValue<TemplateFileMap>> TEMPLATE_CACHE_TWIG = new Key<>("TEMPLATE_CACHE_TWIG");
    private static final Key<CachedValue<TemplateFileMap>> TEMPLATE_CACHE_ALL = new Key<>("TEMPLATE_CACHE_ALL");

    public static final String DOC_SEE_REGEX  = "\\{#[\\s]+@see[\\s]+([-@\\./\\:\\w\\\\\\[\\]]+)[\\s]*#}";
    public static final String DOC_SEE_REGEX_WITHOUT_SEE  = "\\{#[\\s]+([-@\\./\\:\\w\\\\\\[\\]]+)[\\s]*#}";

    /**
     * ([) "FOO", 'FOO' (])
     */
    public static final ElementPattern<PsiElement> STRING_WRAP_PATTERN = PlatformPatterns.or(
        PlatformPatterns.psiElement(PsiWhiteSpace.class),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE)
    );

    @Deprecated
    public static Map<String, VirtualFile> getTemplateFilesByName(@NotNull Project project, boolean useTwig, boolean usePhp) {
        return getTemplateMap(project, useTwig, usePhp).getTemplates();
    }

    @NotNull
    public static synchronized TemplateFileMap getTemplateMap(@NotNull Project project, boolean useTwig, final boolean usePhp) {

        TemplateFileMap templateMapProxy = null;

        // cache twig and all files,
        // only PHP files we dont need to cache
        if(useTwig && !usePhp) {
            // cache twig files only, most use case
            CachedValue<TemplateFileMap> cache = project.getUserData(TEMPLATE_CACHE_TWIG);
            if (cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new MyTwigOnlyTemplateFileMapCachedValueProvider(project), false);
                project.putUserData(TEMPLATE_CACHE_TWIG, cache);
            }

            templateMapProxy = cache.getValue();

        } else if(useTwig && usePhp) {
            // cache all files
            CachedValue<TemplateFileMap> cache = project.getUserData(TEMPLATE_CACHE_ALL);
            if (cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new MyAllTemplateFileMapCachedValueProvider(project), false);
                project.putUserData(TEMPLATE_CACHE_ALL, cache);
            }

            templateMapProxy = cache.getValue();
        }

        // cache-less calls
        if(templateMapProxy == null) {
            templateMapProxy = getTemplateMapProxy(project, useTwig, usePhp);
        }

        return templateMapProxy;
    }

    @NotNull
    private static TemplateFileMap getTemplateMapProxy(@NotNull Project project, boolean useTwig, boolean usePhp) {

        List<TwigPath> twigPaths = new ArrayList<>();
        twigPaths.addAll(getTwigNamespaces(project));

        if(twigPaths.size() == 0) {
            return new TemplateFileMap();
        }

        // app/Resources/ParentBundle/Resources/views
        Map<String, SymfonyBundle> parentBundles = new SymfonyBundleUtil(project).getParentBundles();
        if(parentBundles.size() > 0) {
            for (Map.Entry<String, SymfonyBundle> entry : parentBundles.entrySet()) {
                VirtualFile views = entry.getValue().getRelative("Resources/views");
                if(views != null) {
                    twigPaths.add(new TwigPath(views.getPath(), entry.getKey(), TwigPathIndex.NamespaceType.BUNDLE));
                }
            }
        }

        // app/Resources/FooBundle/views
        VirtualFile relativeFile = VfsUtil.findRelativeFile(project.getBaseDir(), "app", "Resources");
        if(relativeFile != null) {
            for (VirtualFile virtualFile : relativeFile.getChildren()) {

                if(!virtualFile.isDirectory() || !virtualFile.getName().endsWith("Bundle")) {
                    continue;
                }

                VirtualFile views = virtualFile.findChild("views");
                if(views == null) {
                    continue;
                }

                twigPaths.add(new TwigPath(views.getPath(), virtualFile.getName(), TwigPathIndex.NamespaceType.BUNDLE));
            }
        }

        TemplateFileMap container = new TemplateFileMap();

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

                    container.putAll(twigPathContentIterator.getResults());
                }
            }

        }
        
        return container;
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

        Collection<PsiFile> psiFiles = new HashSet<>();

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

        psiFiles.addAll(getTemplateOverwrites(project, normalizedTemplateName));

        return psiFiles.toArray(new PsiFile[psiFiles.size()]);
    }

    /**
     * Collects overwritten templates
     *
     * app/Resources/MyUserBundle/views/layout.html.twig
     * src/Acme/UserBundle/Resources/views/layout.html.twig <- getParent = MyUserBundle
     */
    private static Collection<PsiFile> getTemplateOverwrites(@NotNull Project project, @NotNull String normalizedTemplateName) {

        // Bundle overwrite:
        if(normalizedTemplateName.startsWith(":") || normalizedTemplateName.startsWith("@")) {
            return Collections.emptyList();
        }

        String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/").replace("//", "/"), "/");

        int i = templatePath.indexOf("Bundle/");
        if( i == -1) {
            return Collections.emptyList();
        }

        Collection<VirtualFile> files = new HashSet<>();

        String bundle = templatePath.substring(0, i + 6);

        // invalid Bundle in path condition
        if(bundle.contains("/")) {
            return Collections.emptyList();
        }

        VirtualFile relativeFile = VfsUtil.findRelativeFile(
            project.getBaseDir(),
            String.format("app/Resources/%s/views/%s", bundle, templatePath.substring(i + 7)).split("/")
        );

        if(relativeFile != null) {
            files.add(relativeFile);
        }

        // find parent bundles
        for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundles()) {
            String parentBundle = symfonyBundle.getParentBundleName();
            if(parentBundle != null && bundle.equals(parentBundle)) {
                relativeFile = symfonyBundle.getRelative(String.format("Resources/views/%s", templatePath.substring(i + 7)));
                if(relativeFile != null) {
                    files.add(relativeFile);
                }
            }
        }

        return PsiElementUtils.convertVirtualFilesToPsiFiles(project, files);
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
        List<TwigPath> twigPaths = new ArrayList<>();
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

        // load extension
        TwigNamespaceExtensionParameter parameter = new TwigNamespaceExtensionParameter(project);
        for (TwigNamespaceExtension namespaceExtension : EXTENSIONS.getExtensions()) {
            twigPaths.addAll(namespaceExtension.getNamespaces(parameter));
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
     * {% include ['', ~ '', ''] %}
     */
    public static ElementPattern<PsiElement> getIncludeTagArrayPattern() {
        //noinspection unchecked
        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.INCLUDE_TAG)
            )
            .afterLeafSkipping(
                STRING_WRAP_PATTERN,
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ)
                )
            )
            .beforeLeafSkipping(
                STRING_WRAP_PATTERN,
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                    PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ)
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);

    }

    /**
     * {% include foo ? '' : '' %}
     * {% extends foo ? '' : '' %}
     */
    public static ElementPattern<PsiElement> getTagTernaryPattern(@NotNull IElementType type) {
        //noinspection unchecked
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(type)
                )
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.QUESTION)
                )
                .withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(type)
                )
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
                .withLanguage(TwigLanguage.INSTANCE)
        );
    }
    /**
     * Check for {{ include('|')  }}, {% include('|') %}
     *
     * @param functionName twig function name
     */
    public static ElementPattern<PsiElement> getPrintBlockOrTagFunctionPattern(String... functionName) {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
                    PlatformPatterns.psiElement(TwigElementTypes.TAG)
                )
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
     * {% render "foo"
     *
     * @param tagName twig tag name
     */
    public static ElementPattern<PsiElement> getStringAfterTagNamePattern(@NotNull String tagName) {
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
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.TAG)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Check for {% if foo is "foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenPattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.IS),
                    PlatformPatterns.psiElement(TwigTokenTypes.NOT)
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% if foo <carpet> %}
     * {% if foo.bar <carpet> %}
     * {% if "foo.bar" <carpet> %}
     * {% if 'foo.bar' <carpet> %}
     */
    public static ElementPattern<PsiElement> getAfterOperatorPattern() {
        // @TODO: make it some nicer. can wrap it with whitespace

        //noinspection unchecked
        ElementPattern<PsiElement> or = PlatformPatterns.or(
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
            PlatformPatterns.psiElement(TwigTokenTypes.DOT),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE),
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ),
            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ),
            PlatformPatterns.psiElement(TwigTokenTypes.NUMBER),
            PlatformPatterns.psiElement(TwigTokenTypes.FILTER)
        );

        //noinspection unchecked
        ElementPattern<PsiElement> anIf = PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("if"),
            PlatformPatterns.psiElement(TwigTokenTypes.AND),
            PlatformPatterns.psiElement(TwigTokenTypes.OR)
        );

        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeaf(PlatformPatterns.not(
                PlatformPatterns.psiElement(TwigTokenTypes.DOT)
            ))
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
            )
            .afterLeafSkipping(or, anIf)
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Twig tag pattern with some hack
     * because we have invalid psi elements after STATEMENT_BLOCK_START
     *
     * {% <carpet> %}
     */
    public static ElementPattern<PsiElement> getTagTokenParserPattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_START),
                    PlatformPatterns.psiElement(PsiErrorElement.class)
                )
            )
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_END)
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
            .withParent(PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
                PlatformPatterns.psiElement(TwigElementTypes.SET_TAG)
            ))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% block 'foo' %}
     * {% block "foo" %}
     * {% block foo %}
     */
    public static ElementPattern<PsiElement> getBlockTagPattern() {
        //noinspection unchecked
        return PlatformPatterns.or(

            // {% block "foo" %}
            PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
            )
            .withParent(
                PlatformPatterns.psiElement(TwigBlockTag.class)
            )
            .withLanguage(TwigLanguage.INSTANCE),

            // {% block foo %}
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                )
                .withParent(
                    PlatformPatterns.psiElement(TwigBlockTag.class)
                )
                .withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * use getStringAfterTagNamePattern @TODO
     *
     * {% trans_default_domain '<carpet>' %}
     * {% trans_default_domain <carpet> %}
     */
    public static ElementPattern<PsiElement> getTransDefaultDomainPattern() {
        //noinspection unchecked
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG)
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
                    PlatformPatterns.psiElement(TwigElementTypes.TAG)
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
     * {% trans with {'%name%': 'Fabien'} from "app" %}
     * {% transchoice count with {'%name%': 'Fabien'} from "app" %}
     */
    public static ElementPattern<PsiElement> getTranslationTokenTagFromPattern() {
        //noinspection unchecked

        // we need to use withText check, because twig tags dont have children to search for tag name
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG).withText(
                        PlatformPatterns.string().matches("\\{%\\s+(trans|transchoice).*")
                    )
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("from")
                ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG).withText(
                        PlatformPatterns.string().matches("\\{%\\s+(trans|transchoice).*")
                    )
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("from")
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

    /**
     * {{ foo.fo<caret>o }}
     */
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
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(TwigTypeResolveUtil.DEPRECATED_DOC_TYPE_PATTERN)).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE)).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * {# @see Foo.html.twig #}
     * {# @see \Class #}
     * {# \Class #}
     */
    @NotNull
    public static ElementPattern<PsiComment> getTwigDocSeePattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(DOC_SEE_REGEX)).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiComment().withText(PlatformPatterns.string().matches(DOC_SEE_REGEX_WITHOUT_SEE)).withLanguage(TwigLanguage.INSTANCE)
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

    /**
     *  {{ asset('<caret>') }}
     *  {{ asset("<caret>") }}
     *  {{ absolute_url("<caret>") }}
     */
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
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("asset"),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("absolute_url")
                )
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

    /**
     * {% from _self import foo %}
     * {% from 'template_name' import foo %}
     */
    public static ElementPattern<PsiElement> getFromTemplateElement() {
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("from"))
                )
                .withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns
                .psiElement(TwigTokenTypes.RESERVED_ID)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("from"))
                )
                .withLanguage(TwigLanguage.INSTANCE)
        );

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

    public static Set<VirtualFile> resolveAssetsFiles(Project project, String templateName, String... fileTypes) {


        Set<VirtualFile> virtualFiles = new HashSet<>();

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

        // dont matches wildcard:
        // {% javascripts '@SampleBundle/Resources/public/js/*' %}
        // {% javascripts 'assets/js/*' %}
        // {% javascripts 'assets/js/*.js' %}
        Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(templateName);
        if (!matcher.find()) {

            // directly resolve
            VirtualFile projectAssetRoot = AssetDirectoryReader.getProjectAssetRoot(project);
            if(projectAssetRoot != null) {
                VirtualFile relativeFile = VfsUtil.findRelativeFile(projectAssetRoot, templateName);
                if(relativeFile != null) {
                    virtualFiles.add(relativeFile);
                }
            }

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

        final Collection<PsiElement> targets = new ArrayList<>();

        FileBasedIndexImpl.getInstance().getFilesWithKey(TwigMacroFunctionStubIndex.KEY, new HashSet<>(Arrays.asList(name)), virtualFile -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile != null) {
                PsiTreeUtil.processElements(psiFile, new PsiElementProcessor() {
                    public boolean execute(@NotNull PsiElement psiElement) {

                        if (getTwigMacroNameKnownPattern(name).accepts(psiElement)) {
                            targets.add(psiElement);
                        }

                        return true;

                    }
                });
            }

            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        return targets;
    }

    public static Collection<LookupElement> getTwigLookupElements(Project project) {
        VirtualFile baseDir = project.getBaseDir();

        Collection<LookupElement> lookupElements = new ArrayList<>();

        for (Map.Entry<String, VirtualFile> entry : TwigHelper.getTwigFilesByName(project).entrySet()) {
            lookupElements.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue(), baseDir)
            );
        }

        return lookupElements;
    }

    public static Collection<LookupElement> getAllTemplateLookupElements(Project project) {
        VirtualFile baseDir = project.getBaseDir();

        Collection<LookupElement> lookupElements = new ArrayList<>();

        for (Map.Entry<String, VirtualFile> entry : TwigHelper.getTemplateFilesByName(project).entrySet()) {
            lookupElements.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue(), baseDir)
            );
        }

        return lookupElements;
    }

    /**
     * {% include 'foo.html.twig' %}
     * {% include ['foo.html.twig', 'foo_1.html.twig'] %}
     */
    @NotNull
    public static Collection<String> getIncludeTagStrings(@NotNull TwigTagWithFileReference twigTagWithFileReference) {

        if(twigTagWithFileReference.getNode().getElementType() != TwigElementTypes.INCLUDE_TAG) {
            return Collections.emptySet();
        }

        Collection<String> strings = new LinkedHashSet<>();
        PsiElement firstChild = twigTagWithFileReference.getFirstChild();
        if(firstChild == null) {
            return strings;
        }

        // {% include 'foo.html.twig' %}
        PsiElement psiSingleString = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                )
        );

        // single match dont need to go deeper in conditional check, so stop here
        if(psiSingleString != null) {
            String text = psiSingleString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
            return strings;
        }

        // {% include ['foo.html.twig', 'foo_1.html.twig'] %}
        PsiElement arrayMatch = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ));
        if(arrayMatch != null) {

            // match: "([,)''(,])"
            Collection<PsiElement> questString = PsiElementUtils.getNextSiblingOfTypes(arrayMatch, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                    .afterLeafSkipping(
                        STRING_WRAP_PATTERN,
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ)
                        )
                    )
                    .beforeLeafSkipping(
                        STRING_WRAP_PATTERN,
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ)
                        )
                    )
            );

            for (PsiElement psiElement : questString) {
                String text = psiElement.getText();
                if(StringUtils.isNotBlank(text)) {
                    strings.add(text);
                }
            }
        }

        PsiElement psiQuestion = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.QUESTION));
        if(psiQuestion != null) {
            strings.addAll(getTernaryStrings(psiQuestion));
        }

        return strings;

    }

    /**
     * Find "extends" template in twig TwigExtendsTag
     *
     * {% extends '::base.html.twig' %}
     * {% extends request.ajax ? "base_ajax.html" : "base.html" %}
     *
     * @param twigExtendsTag Extends tag
     * @return valid template names
     */
    @NotNull
    public static Collection<String> getTwigExtendsTagTemplates(@NotNull TwigExtendsTag twigExtendsTag) {

        Collection<String> strings = new HashSet<>();
        PsiElement firstChild = twigExtendsTag.getFirstChild();
        if(firstChild == null) {
            return strings;
        }

        // single {% extends '::base.html.twig'
        PsiElement psiSingleString = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                )
        );

        // single match dont need to go deeper in conditional check, so stop here
        if(psiSingleString != null) {
            String text = psiSingleString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
            return strings;
        }

        PsiElement psiQuestion = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.QUESTION));
        if(psiQuestion != null) {
            strings.addAll(getTernaryStrings(psiQuestion));
        }

        return strings;
    }

    /**
     * "foo ? 'foo' : 'bar'"
     */
    private static Collection<String> getTernaryStrings(@NotNull PsiElement psiQuestion) {

        Collection<String> strings = new TreeSet<>();

        // match ? "foo" :
        PsiElement questString = PsiElementUtils.getNextSiblingOfType(psiQuestion, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.QUESTION)
                )
                .beforeLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
        );

        if(questString != null) {
            String text = questString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
        }

        // : "foo"
        PsiElement colonString = PsiElementUtils.getNextSiblingOfType(psiQuestion, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
        );

        if(colonString != null) {
            String text = colonString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
        }

        return strings;
    }

    /**
     * Collect all block names in file
     *
     * {% block sds %}, {% block 'sds' %}, {% block "sds" %}
     * {%- block sds -%}
     */
    @NotNull
    public static Collection<TwigBlock> getBlocksInFile(@NotNull TwigFile twigFile) {

        Collection<TwigBlock> block = new ArrayList<>();

        PsiElementPattern.Capture<PsiElement> pattern = null;

        for (TwigBlockTag twigBlockTag : PsiTreeUtil.collectElementsOfType(twigFile, TwigBlockTag.class)) {

            String name = twigBlockTag.getName();
            if(name != null && StringUtils.isNotBlank(name)) {
                block.add(new TwigBlock(name, twigBlockTag));
            }

            PsiElement firstChild = twigBlockTag.getFirstChild();
            if(firstChild == null) {
                continue;
            }

            // provide support for quote wrapping
            // {% block 'sds' %}
            if(pattern == null) {
                pattern = PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                    .afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                    );
            }

            PsiElement psiString = PsiElementUtils.getNextSiblingOfType(firstChild, pattern);
            if(psiString != null) {
                String text = psiString.getText();
                if(StringUtils.isNotBlank(text)) {
                    block.add(new TwigBlock(text, twigBlockTag));
                }
            }

        }

        return block;
    }

    private static class MyTwigOnlyTemplateFileMapCachedValueProvider implements CachedValueProvider<TemplateFileMap> {

        private final Project project;

        public MyTwigOnlyTemplateFileMapCachedValueProvider(Project project) {
            this.project = project;
        }

        @Nullable
        @Override
        public Result<TemplateFileMap> compute() {
            return Result.create(getTemplateMapProxy(project, true, false), PsiModificationTracker.MODIFICATION_COUNT);
        }
    }

    private static class MyAllTemplateFileMapCachedValueProvider implements CachedValueProvider<TemplateFileMap> {

        private final Project project;

        public MyAllTemplateFileMapCachedValueProvider(Project project) {
            this.project = project;
        }

        @Nullable
        @Override
        public Result<TemplateFileMap> compute() {
            return Result.create(getTemplateMapProxy(project, true, true), PsiModificationTracker.MODIFICATION_COUNT);
        }
    }

    /**
     * Find block scope "embed" with self search or file context with foreign extends search
     *
     * {% embed "template.twig" %}{% block <caret> %}
     * {% block <caret> %}
     */
    @NotNull
    public static Pair<PsiFile[], Boolean> findScopedFile(@NotNull PsiElement psiElement) {

        // {% embed "template.twig" %}{% block <caret> %}
        PsiElement firstParent = TwigUtil.getTransDefaultDomainScope(psiElement);

        // {% embed "template.twig" %}
        if(firstParent != null && firstParent.getNode().getElementType() == TwigElementTypes.EMBED_STATEMENT) {
            PsiElement embedTag = firstParent.getFirstChild();
            if(embedTag.getNode().getElementType() == TwigElementTypes.EMBED_TAG) {
                PsiElement fileReference = ContainerUtil.find(YamlHelper.getChildrenFix(embedTag), psiElement12 -> {
                    return TwigHelper.getTemplateFileReferenceTagPattern().accepts(psiElement12);
                });

                if(fileReference != null && TwigUtil.isValidTemplateString(fileReference)) {
                    String text = fileReference.getText();
                    if(StringUtils.isNotBlank(text)) {
                        return Pair.create(
                            TwigHelper.getTemplatePsiElements(psiElement.getProject(), text),
                            true
                        );
                    }
                }
            }

            return Pair.create(new PsiFile[] {}, true);
        }

        return Pair.create(new PsiFile[] {psiElement.getContainingFile()}, false);
    }

    /**
     * Collects Twig path in given yaml configuration
     *
     * twig:
     *  paths:
     *   "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
     */
    @NotNull
    public static Collection<Pair<String, String>> getTwigPathFromYamlConfig(@NotNull YAMLFile yamlFile) {
        YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile(yamlFile, "twig", "paths");
        if(yamlKeyValue == null) {
            return Collections.emptyList();
        }

        YAMLValue value = yamlKeyValue.getValue();
        if(!(value instanceof YAMLMapping)) {
            return Collections.emptyList();
        }

        Collection<Pair<String, String>> pair = new ArrayList<>();

        for (YAMLPsiElement element : value.getYAMLElements()) {
            if(!(element instanceof YAMLKeyValue)) {
                continue;
            }

            String keyText = ((YAMLKeyValue) element).getKeyText();
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            String valueText = ((YAMLKeyValue) element).getValueText();

            // normalize null value
            if(valueText.equals("~")) {
                valueText = "";
            }

            pair.add(Pair.create(valueText, keyText));
        }

        return pair;
    }
}
