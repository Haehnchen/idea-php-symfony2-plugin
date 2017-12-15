package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.*;
import com.intellij.util.Consumer;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public static String[] IMG_FILES_EXTENSIONS = new String[] { "png", "jpg", "jpeg", "gif", "svg"};

    public static String TEMPLATE_ANNOTATION_CLASS = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template";

    private static final Key<CachedValue<Map<String, Set<VirtualFile>>>> TEMPLATE_CACHE_TWIG = new Key<>("TEMPLATE_CACHE_TWIG");
    private static final Key<CachedValue<Map<String, Set<VirtualFile>>>> TEMPLATE_CACHE_ALL = new Key<>("TEMPLATE_CACHE_ALL");

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

    private static final ElementPattern[] PARAMETER_WHITE_LIST = new ElementPattern[]{
        PlatformPatterns.psiElement(PsiWhiteSpace.class),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.NUMBER),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.CONCAT),
        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
        PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
        PlatformPatterns.psiElement(TwigTokenTypes.DOT)
    };

    /**
     * Generate a mapped template name file multiple relation:
     *
     * foo.html.twig => ["views/foo.html.twig", "templates/foo.html.twig"]
     */
    @NotNull
    private static synchronized Map<String, Set<VirtualFile>> getTemplateMap(@NotNull Project project, boolean useTwig, final boolean usePhp) {
        Map<String, Set<VirtualFile>> templateMapProxy = null;

        // cache twig and all files,
        // only PHP files we dont need to cache
        if(useTwig && !usePhp) {
            // cache twig files only, most use case
            CachedValue<Map<String, Set<VirtualFile>>> cache = project.getUserData(TEMPLATE_CACHE_TWIG);
            if (cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new MyTwigOnlyTemplateFileMapCachedValueProvider(project), false);
                project.putUserData(TEMPLATE_CACHE_TWIG, cache);
            }

            templateMapProxy = cache.getValue();

        } else if(useTwig && usePhp) {
            // cache all files
            CachedValue<Map<String, Set<VirtualFile>>> cache = project.getUserData(TEMPLATE_CACHE_ALL);
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
    private static Map<String, Set<VirtualFile>> getTemplateMapProxy(@NotNull Project project, boolean useTwig, boolean usePhp) {
        List<TwigPath> twigPaths = new ArrayList<>(getTwigNamespaces(project));
        if(twigPaths.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, Set<VirtualFile>> templateNames = new HashMap<>();

        for (TwigPath twigPath : twigPaths) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            VirtualFile virtualDirectoryFile = twigPath.getDirectory(project);
            if(virtualDirectoryFile != null) {
                MyLimitedVirtualFileVisitor visitor = new MyLimitedVirtualFileVisitor(project, twigPath, usePhp, useTwig, 5, 150);

                VfsUtil.visitChildrenRecursively(virtualDirectoryFile, visitor);

                for (Map.Entry<String, VirtualFile> entry : visitor.getResults().entrySet()) {
                    if(!templateNames.containsKey(entry.getKey())) {
                        templateNames.put(entry.getKey(), new HashSet<>());
                    }

                    templateNames.get(entry.getKey()).add(entry.getValue());
                }
            }
        }
        
        return templateNames;
    }

    @NotNull
    private static Map<String, Set<VirtualFile>> getTwigTemplateFiles(@NotNull Project project) {
        return getTemplateMap(project, true, false);
    }

    @NotNull
    public static Collection<String> getTwigFileNames(@NotNull Project project) {
        return getTemplateMap(project, true, false).keySet();
    }

    @NotNull
    public static Map<String, Set<VirtualFile>> getTwigAndPhpTemplateFiles(@NotNull Project project) {
        return getTemplateMap(project, true, true);
    }

    @Nullable
    private static TwigNamespaceSetting findManagedTwigNamespace(@NotNull Project project, @NotNull TwigPath twigPath) {
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

    /**
     * Normalize incoming template names. Provide normalization on indexing and resolving
     *
     * BarBundle:Foo:steps/step_finish.html.twig
     * BarBundle:Foo/steps:step_finish.html.twig
     * "@!Bar/step_finish.html.twig"
     *
     * todo: provide setting for that
     */
    public static String normalizeTemplateName(@NotNull String templateName) {
        // force linux path style
        templateName = templateName.replace("\\", "/");

        if(templateName.startsWith("@") || !templateName.matches("^.*?:.*?:.*?/.*?$")) {
            // Symfony 3.4 overwrite
            // {% extends '@!FOSUser/layout.html.twig' %}
            if(templateName.startsWith("@!")) {
                templateName = "@" + templateName.substring(2);
            }

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
    @NotNull
    public static PsiFile[] getTemplatePsiElements(@NotNull Project project, @NotNull String templateName) {
        String normalizedTemplateName = normalizeTemplateName(templateName);

        Collection<VirtualFile> virtualFiles = new HashSet<>();
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
                            addFileInsideTwigPath(project, normalizedTemplateName.substring(i + 1), virtualFiles, twigPath);
                        }
                    }
                }
            } else if(normalizedTemplateName.startsWith(":")) {
                // ::base.html.twig
                // :Foo:base.html.twig
                if(normalizedTemplateName.length() > 1 && twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE && twigPath.isGlobalNamespace()) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
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
                            addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                        }

                    }
                }

                // form_div_layout.html.twig
                if(twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.ADD_PATH) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                }

                // Bundle overwrite:
                // FooBundle:index.html -> app/views/FooBundle:index.html
                if(twigPath.isGlobalNamespace() && !normalizedTemplateName.startsWith(":") && !normalizedTemplateName.startsWith("@")) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/").replace("//", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                }
            }
        }

        Collection<PsiFile> psiFiles = PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles);
        return psiFiles.toArray(new PsiFile[psiFiles.size()]);
    }

    /**
     * Switch template file or path target on caret offset "foo/bar.html.twig" or "foo"
     *
     * "foo" "bar.html.twig"
     */
    @NotNull
    public static Collection<PsiElement> getTemplateNavigationOnOffset(@NotNull Project project, @NotNull String templateName, int offset) {
        Set<PsiElement> files = new HashSet<>();

        // try to find a path pattern on current offset after path normalization
        if(offset < templateName.length()) {
            String templateNameWithCaret = normalizeTemplateName(new StringBuilder(templateName).insert(offset, '\u0182').toString());
            offset = templateNameWithCaret.indexOf('\u0182');

            int i = StringUtils.strip(templateNameWithCaret.replace(String.valueOf('\u0182'), "").replace(":", "/"), "/").indexOf("/", offset);
            if(i > 0) {
                files.addAll(getTemplateTargetOnOffset(project, templateName, offset));
            }
        }

        // full filepath fallback: "foo/foo<caret>.html.twig"
        if(files.size() == 0) {
            files.addAll(Arrays.asList(getTemplatePsiElements(project, templateName)));
        }

        return files;
    }

    /**
     * Switch template target on caret offset "foo/bar.html.twig". Resolve template name or directory structure:
     *
     * "foo" "bar.html.twig"
     */
    @NotNull
    public static Collection<PsiElement> getTemplateTargetOnOffset(@NotNull Project project, @NotNull String templateName, int offset) {
        // no match for length
        if(offset > templateName.length()) {
            return Collections.emptyList();
        }

        // please give use a normalized path:
        // Foo:foo:foo => foo/foo/foo
        String templatePathWithFileName = normalizeTemplateName(new StringBuilder(templateName).insert(offset, '\u0182').toString());
        offset = templatePathWithFileName.indexOf('\u0182');

        int indexOf = templatePathWithFileName.replace(":", "/").indexOf("/", offset);
        if(indexOf <= 0) {
            return Collections.emptyList();
        }

        String templatePath = StringUtils.strip(templatePathWithFileName.substring(0, indexOf).replace(String.valueOf('\u0182'), ""), "/");

        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (TwigPath twigPath : getTwigNamespaces(project)) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            if(templatePath.startsWith("@")) {
                // @Namespace/base.html.twig
                // @Namespace/folder/base.html.twig
                if(templatePath.length() > 1 && twigPath.getNamespaceType() != TwigPathIndex.NamespaceType.BUNDLE) {
                    int x = templatePath.indexOf("/");

                    if(x < 0 && templatePath.substring(1).equals(twigPath.getNamespace())) {
                        // Click on namespace itself: "@Foobar"
                        VirtualFile relativeFile = twigPath.getDirectory(project);
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    } else if (x > 0 && templatePath.substring(1, x).equals(twigPath.getNamespace())) {
                        // Click on path: "@Foobar/Foo"
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.substring(x + 1).split("/"));
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    }
                }
            } else if(templatePath.startsWith(":")) {
                // ::base.html.twig
                // :Foo:base.html.twig
                if(twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE && twigPath.isGlobalNamespace()) {
                    String replace = StringUtils.strip(templatePath.replace(":", "/"), "/");

                    VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), replace.split("/"));
                    if(relativeFile != null) {
                        virtualFiles.add(relativeFile);
                    }
                }
            } else {
                // FooBundle::base.html.twig
                // FooBundle:Bar:base.html.twig
                if(twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE) {
                    templatePath = templatePath.replace(":", "/");
                    int x = templatePath.indexOf("/");

                    if(x < 0 && templatePath.equals(twigPath.getNamespace())) {
                        // Click on namespace itself: "FooBundle"
                        VirtualFile relativeFile = twigPath.getDirectory(project);
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    } else if(x > 0 && templatePath.substring(0, x).equals(twigPath.getNamespace())) {
                        // Click on path: "FooBundle/Foo"
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.substring(x + 1).split("/"));
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    }
                }

                // form_div_layout.html.twig
                if(twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.ADD_PATH) {
                    VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.split("/"));
                    if(relativeFile != null) {
                        virtualFiles.add(relativeFile);
                    }
                }

                // Bundle overwrite:
                // FooBundle:index.html -> app/views/FooBundle:index.html
                if(twigPath.isGlobalNamespace() && !templatePath.startsWith(":") && !templatePath.startsWith("@")) {
                    // @TODO: support this later on; also its deprecated by Symfony
                }
            }
        }

        return virtualFiles
            .stream()
            .map(virtualFile -> PsiManager.getInstance(project).findDirectory(virtualFile))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Resolve TwigFile to its possible template names:
     *
     * "@Foo/test.html.twig"
     * "test.html.twig"
     * "::test.html.twig"
     */
    @NotNull
    public static Collection<String> getTemplateNamesForFile(@NotNull TwigFile twigFile) {
        return getTemplateNamesForFile(twigFile.getProject(), twigFile.getVirtualFile());
    }

    /**
     * Resolve VirtualFile to its possible template names:
     *
     * "@Foo/test.html.twig"
     * "test.html.twig"
     * "::test.html.twig"
     */
    @NotNull
    public static Collection<String> getTemplateNamesForFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        List<TwigPath> collect = getTwigNamespaces(project, true)
            .stream()
            .filter(TwigPath::isEnabled)
            .collect(Collectors.toList());

        Collection<String> templates = new ArrayList<>();

        for (TwigPath twigPath : collect) {
            String templateName = getTemplateNameForTwigPath(project, twigPath, virtualFile);
            if(templateName == null) {
                continue;
            }

            templates.add(templateName);
        }

        return templates;
    }

    @Nullable
    static String getTemplateNameForTwigPath(@NotNull Project project, @NotNull TwigPath twigPath, @NotNull VirtualFile virtualFile) {
        VirtualFile directory = twigPath.getDirectory(project);
        if(directory == null) {
            return null;
        }

        String templatePath = VfsUtil.getRelativePath(virtualFile, directory, '/');
        if(templatePath == null) {
            return null;
        }

        String templateDirectory; // xxx:XXX:xxx
        String templateFile; // xxx:xxx:XXX

        if (templatePath.contains("/")) {
            int lastDirectorySeparatorIndex = templatePath.lastIndexOf("/");
            templateDirectory = templatePath.substring(0, lastDirectorySeparatorIndex);
            templateFile = templatePath.substring(lastDirectorySeparatorIndex + 1);
        } else {
            templateDirectory = "";
            templateFile = templatePath;
        }

        String namespace = twigPath.getNamespace().equals(TwigPathIndex.MAIN) ? "" : twigPath.getNamespace();

        String templateFinalName;
        if(twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE) {
            templateFinalName = namespace + ":" + templateDirectory + ":" + templateFile;
        } else {
            templateFinalName = namespace + "/" + templateDirectory + "/" + templateFile;

            // remove empty path and check for root (global namespace)
            templateFinalName = templateFinalName.replace("//", "/");
            if(templateFinalName.startsWith("/")) {
                templateFinalName = templateFinalName.substring(1);
            } else {
                templateFinalName = "@" + templateFinalName;
            }
        }

        return templateFinalName;
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

    private static void addFileInsideTwigPath(@NotNull Project project, @NotNull String templatePath, @NotNull Collection<VirtualFile> virtualFiles, @NotNull TwigPath twigPath) {
        VirtualFile virtualFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.split("/"));

        if(virtualFile != null) {
            virtualFiles.add(virtualFile);
        }
    }

    public static List<TwigPath> getTwigNamespaces(@NotNull Project project) {
       return getTwigNamespaces(project, true);
    }

    @NotNull
    public static List<TwigPath> getTwigNamespaces(@NotNull Project project, boolean includeSettings) {
        List<TwigPath> twigPaths = new ArrayList<>();

        // load extension
        TwigNamespaceExtensionParameter parameter = new TwigNamespaceExtensionParameter(project);
        for (TwigNamespaceExtension namespaceExtension : EXTENSIONS.getExtensions()) {
            twigPaths.addAll(namespaceExtension.getNamespaces(parameter));
        }

        // disable namespace explicitly disabled by user
        for(TwigPath twigPath: twigPaths) {
            TwigNamespaceSetting twigNamespaceSetting = findManagedTwigNamespace(project, twigPath);
            if(twigNamespaceSetting != null) {
                twigPath.setEnabled(false);
            }
        }

        twigPaths = getUniqueTwigTemplatesList(twigPaths);

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

    /**
     * Build a unique path + namespace + type list
     * normalize also windows linux path
     */
    @NotNull
    public static List<TwigPath> getUniqueTwigTemplatesList(@NotNull Collection<TwigPath> origin) {
        List<TwigPath> twigPaths = new ArrayList<>();

        Set<String> hashes = new HashSet<>();
        for (TwigPath twigPath : origin) {
            // normalize hash; for same path element
            // TODO: move to path object itself
            String hash = twigPath.getNamespaceType() + twigPath.getNamespace() + twigPath.getPath().replace("\\", "/");
            if(hashes.contains(hash)) {
                continue;
            }

            twigPaths.add(twigPath);
            hashes.add(hash);
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
                    PlatformPatterns.psiElement(TwigElementTypes.TAG),
                    PlatformPatterns.psiElement(TwigElementTypes.IF_TAG),
                    PlatformPatterns.psiElement(TwigElementTypes.SET_TAG),
                    PlatformPatterns.psiElement(TwigElementTypes.ELSE_TAG),
                    PlatformPatterns.psiElement(TwigElementTypes.ELSEIF_TAG)
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
     * Literal are fine in lexer so just extract the parameter
     *
     * {{ foo({'foobar', 'foo<caret>bar'}) }}
     * {{ foo({'fo<caret>obar'}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsLiteralPattern(@NotNull String... functionName) {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ foo({'foo<caret>bar': 'foo'}}) }}
     * {{ foo({'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsKeyLiteralPattern(@NotNull String... functionName) {
        return PlatformPatterns.or(
            PlatformPatterns
                // ",'foo'", {'foo'"
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL).withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                    )
                )
            ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns
                // ",'foo'", {'foo'"
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.psiElement().with(new MyBeforeColonAndInsideLiteralPatternCondition()),
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
            )
        );
    }

    /**
     * {{ foo(12, {'foo<caret>bar': 'foo'}}) }}
     * {{ foo(12, {'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithSecondParameterAsKeyLiteralPattern(@NotNull String... functionName) {
        //noinspection unchecked
        PsiElementPattern.Capture<PsiElement> parameterPattern = PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                PlatformPatterns.or(PARAMETER_WHITE_LIST),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.NUMBER)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            )
        );

        return
            PlatformPatterns.or(
                // {{ foo({'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).withParent(parameterPattern)
                ).withLanguage(TwigLanguage.INSTANCE),
                // {{ foo(12, {'foo<caret>bar': 'foo'}}) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL).withParent(parameterPattern)
                )
                    .withLanguage(TwigLanguage.INSTANCE)
            );
    }

    /**
     * Array values are not detected by lexer, lets do the magic on our own
     *
     * {{ foo(['foobar', 'foo<caret>bar']) }}
     * {{ foo(['fo<caret>obar']) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsArrayPattern(@NotNull String... functionName) {
        //noinspection unchecked

        // "foo(<caret>"
        PsiElementPattern.Capture<PsiElement> functionPattern = PlatformPatterns
            .psiElement(TwigTokenTypes.LBRACE_SQ)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            );

        return
            PlatformPatterns.or(
                // {{ foo(['fo<caret>obar']) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                        TwigTokenTypes.SINGLE_QUOTE,
                        TwigTokenTypes.DOUBLE_QUOTE
                    )).afterLeafSkipping(
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        functionPattern
                    )
                ).withLanguage(TwigLanguage.INSTANCE),

                // {{ foo(['foobar', 'foo<caret>bar']) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                        TwigTokenTypes.SINGLE_QUOTE,
                        TwigTokenTypes.DOUBLE_QUOTE
                    )).afterLeafSkipping(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                            PlatformPatterns.or(
                                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                                PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
                                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                                PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                            ),
                            functionPattern
                        )
                    )
                ).withLanguage(TwigLanguage.INSTANCE)
            );
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
     * Check for {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenWithOneIdentifierLeafPattern() {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).afterLeafSkipping(PlatformPatterns.psiElement(PsiWhiteSpace.class), PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.IS),
                    PlatformPatterns.psiElement(TwigTokenTypes.NOT)
                ))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Extract text {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenTextPattern() {
        //noinspection unchecked
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.IS),
            PlatformPatterns.psiElement(TwigTokenTypes.NOT)
        );
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
     * {% <caret> %}
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
     * Twig tag pattern
     *
     * {% fo<caret>obar %}
     * {% fo<caret>obar 'foo' %}
     */
    public static ElementPattern<PsiElement> getTagTokenBlockPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.TAG_NAME)
                .withParent(PlatformPatterns.psiElement(TwigElementTypes.TAG))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% FOOBAR "WANTED.html.twig" %}
     */
    public static ElementPattern<PsiElement> getTagNameParameterPattern(@NotNull IElementType elementType, @NotNull String tagName) {
        //noinspection unchecked
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(elementType)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% embed "vertical_boxes_skeleton.twig" %}
     */
    public static ElementPattern<PsiElement> getEmbedPattern() {
        return getTagNameParameterPattern(TwigElementTypes.EMBED_TAG, "embed");
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
     * {% filter foo %}
     */
    public static ElementPattern<PsiElement> getFilterTagPattern() {
        //noinspection unchecked
        return
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
                    PlatformPatterns.psiElement(TwigElementTypes.FILTER_TAG)
                )
                .withLanguage(TwigLanguage.INSTANCE)
            ;
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

        // we need to use withText check, because twig tags dont have childrenAllowToVisit to search for tag name
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
     * trans({}, 'bar')
     * trans(null, 'bar')
     * transchoice(2, null, 'bar')
     */
    public static ElementPattern<PsiElement> getTransDomainPattern() {
        //noinspection unchecked
        ElementPattern[] whitespace = {
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
        };

        ElementPattern[] placeholder = {
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
            PlatformPatterns.psiElement(TwigTokenTypes.DOT),
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ),
            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ)
        };

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
                    // trans({}, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(whitespace),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_CURL).withParent(
                            PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                    PlatformPatterns.or(whitespace),
                                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans"))
                                )
                            )
                        )
                    ),
                    // trans(null, 'bar')
                    // trans(, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(placeholder),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                            PlatformPatterns.or(whitespace),
                            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans"))
                        )
                    ),
                    // transchoice(2, {}, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(whitespace),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_CURL).withParent(
                            PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                                    PlatformPatterns.or(PARAMETER_WHITE_LIST),
                                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                        PlatformPatterns.or(whitespace),
                                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("transchoice"))
                                    )
                                )
                            )
                        )
                    ),
                    // transchoice(2, null, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(placeholder),
                        PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                            PlatformPatterns.or(PARAMETER_WHITE_LIST),
                            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("transchoice"))
                            )
                        )
                    )
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ path('_profiler_info', {'<caret>'}) }}
     * {{ path('_profiler_info', {'foobar': 'foobar', '<caret>'}) }}
     */
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
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
                            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).withParent(
                            PlatformPatterns.psiElement().withText(PlatformPatterns.string().contains("path"))
                        )
                    )
                )
            )
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
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)
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

    /**
     * {{ 'test'|<caret> }}
     */
    public static ElementPattern<PsiElement> getFilterPattern() {
        //noinspection unchecked
        return PlatformPatterns.psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement().withElementType(TwigTokenTypes.FILTER)
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
    public static String getMatchingRouteNameOnParameter(@NotNull PsiElement startPsiElement) {
        PsiElement parent = startPsiElement.getParent();
        if(parent.getNode().getElementType() != TwigElementTypes.LITERAL) {
            return null;
        }

        final String[] text = {null};
        PsiElementUtils.getPrevSiblingOnCallback(parent, psiElement -> {
            IElementType elementType = psiElement.getNode().getElementType();
            if(elementType == TwigTokenTypes.STRING_TEXT) {

                // Only valid string parameter "('foobar',"
                if(getFirstFunctionParameterAsStringPattern().accepts(psiElement)){
                    text[0] = psiElement.getText();
                }

                return false;
            }

            // exit on invalid items
            return
                psiElement instanceof PsiWhiteSpace ||
                elementType == TwigTokenTypes.WHITE_SPACE ||
                elementType == TwigTokenTypes.COMMA ||
                elementType == TwigTokenTypes.SINGLE_QUOTE  ||
                elementType == TwigTokenTypes.DOUBLE_QUOTE
            ;
        });

        return text[0];
    }

    /**
     * Only a parameter is valid "('foobar',"
     */
    @NotNull
    private static PsiElementPattern getFirstFunctionParameterAsStringPattern() {
        // string wrapped elements
        ElementPattern[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };

        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.LBRACE));
    }

    /**
     * Only a parameter is valid ", 'foobar' [,)]"
     */
    @NotNull
    public static PsiElementPattern getParameterAsStringPattern() {
        // string wrapped elements
        ElementPattern[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };

        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.COMMA), PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA));
    }

    @NotNull
    public static Set<String> getTwigMacroSet(Project project) {
        return SymfonyProcessors.createResult(project, TwigMacroFunctionStubIndex.KEY);
    }

    public static Collection<PsiElement> getTwigMacroTargets(final Project project, final String name) {

        final Collection<PsiElement> targets = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(TwigMacroFunctionStubIndex.KEY, new HashSet<>(Collections.singletonList(name)), virtualFile -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile != null) {
                PsiTreeUtil.processElements(psiFile, psiElement -> {
                    if (getTwigMacroNameKnownPattern(name).accepts(psiElement)) {
                        targets.add(psiElement);
                    }

                    return true;

                });
            }

            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        return targets;
    }

    /**
     * Lookup elements for Twig files
     */
    @NotNull
    public static Collection<LookupElement> getTwigLookupElements(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();

        return TwigHelper.getTwigTemplateFiles(project).entrySet().stream()
            .filter(entry -> entry.getValue().size() > 0)
            .map((Function<Map.Entry<String, Set<VirtualFile>>, LookupElement>) entry ->
                new TemplateLookupElement(entry.getKey(), entry.getValue().iterator().next(), baseDir)
            )
            .collect(Collectors.toList());
    }

    /**
     * Lookup elements for Twig and PHP template files
     */
    @NotNull
    public static Collection<LookupElement> getAllTemplateLookupElements(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();

        return TwigHelper.getTwigAndPhpTemplateFiles(project).entrySet().stream()
            .filter(entry -> entry.getValue().size() > 0)
            .map((Function<Map.Entry<String, Set<VirtualFile>>, LookupElement>) entry ->
                new TemplateLookupElement(entry.getKey(), entry.getValue().iterator().next(), baseDir)
            )
            .collect(Collectors.toList());
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
            visitStringInArray(arrayMatch, pair ->
                strings.add(pair.getFirst())
            );
        }

        PsiElement psiQuestion = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.QUESTION));
        if(psiQuestion != null) {
            strings.addAll(getTernaryStrings(psiQuestion));
        }

        return strings;
    }

    /**
     * Visit string values of given array start brace
     * ["foobar"]
     */
    public static void visitStringInArray(@NotNull PsiElement arrayStartBrace, @NotNull Consumer<Pair<String, PsiElement>> pair) {
        // match: "([,)''(,])"
        Collection<PsiElement> questString = PsiElementUtils.getNextSiblingOfTypes(arrayStartBrace, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
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
                pair.consume(Pair.create(text, psiElement));
            }
        }
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
     * {% block sds %}, {% block 'sds' %}, {% block "sds" %}, {%- block sds -%}
     * {{ block('foobar') }}
     */
    @NotNull
    public static Collection<TwigBlock> getBlocksInFile(@NotNull TwigFile twigFile) {
        // prefilter elements; dont visit until leaf elements fpr performance
        PsiElement[] blocks = PsiTreeUtil.collectElements(twigFile, psiElement ->
            psiElement instanceof TwigBlockTag || (psiElement instanceof TwigCompositeElement && psiElement.getNode().getElementType() == TwigElementTypes.PRINT_BLOCK)
        );

        Collection<TwigBlock> block = new ArrayList<>();

        for (PsiElement psiElement : blocks) {
            final PsiElement[] target = new PsiElement[1];

            if(psiElement instanceof TwigBlockTag) {
                // {% block sds %}, {% block "sds" %}
                psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(target[0] == null && getBlockTagPattern().accepts(element)) {
                            target[0] = element;
                        }
                        super.visitElement(element);
                    }
                });

            } else if(psiElement instanceof TwigCompositeElement) {
                // {{ block('foobar') }}
                psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(target[0] == null && getPrintBlockFunctionPattern("block").accepts(element)) {
                            target[0] = element;
                        }
                        super.visitElement(element);
                    }
                });
            }

            if(target[0] == null) {
                continue;
            }

            String blockName = target[0].getText();
            if(StringUtils.isBlank(blockName)) {
                continue;
            }

            block.add(new TwigBlock(blockName, target[0]));
        }

        return block;
    }

    private static class MyTwigOnlyTemplateFileMapCachedValueProvider implements CachedValueProvider<Map<String, Set<VirtualFile>>> {
        @NotNull
        private final Project project;

        MyTwigOnlyTemplateFileMapCachedValueProvider(@NotNull Project project) {
            this.project = project;
        }

        @Nullable
        @Override
        public Result<Map<String, Set<VirtualFile>>> compute() {
            return Result.create(getTemplateMapProxy(project, true, false), PsiModificationTracker.MODIFICATION_COUNT);
        }
    }

    private static class MyAllTemplateFileMapCachedValueProvider implements CachedValueProvider<Map<String, Set<VirtualFile>>> {
        @NotNull
        private final Project project;

        MyAllTemplateFileMapCachedValueProvider(@NotNull Project project) {
            this.project = project;
        }

        @Nullable
        @Override
        public Result<Map<String, Set<VirtualFile>>> compute() {
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
                PsiElement fileReference = ContainerUtil.find(YamlHelper.getChildrenFix(embedTag), psiElement12 ->
                    TwigHelper.getTemplateFileReferenceTagPattern().accepts(psiElement12)
                );

                if(fileReference != null && TwigUtil.isValidStringWithoutInterpolatedOrConcat(fileReference)) {
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

            keyText = keyText.replace("\\", "/").replaceAll("/+", "/");

            // empty value is empty string on out side:
            // "foo: "
            String valueText = "";

            YAMLValue yamlValue = ((YAMLKeyValue) element).getValue();
            if (yamlValue != null) {
                valueText = ((YAMLKeyValue) element).getValueText();
            } else {
                // workaround for foo: !foobar
                // as we got tag element
                PsiElement key = ((YAMLKeyValue) element).getKey();
                if(key != null) {
                    PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(key, PlatformPatterns.psiElement(YAMLTokenTypes.TAG));
                    if(nextSiblingOfType != null) {
                        String text = nextSiblingOfType.getText();
                        if(text.startsWith("!")) {
                            valueText = StringUtils.stripStart(text, "!");
                        }
                    }
                }
            }

            // Symfony 3.4 / 4.0: namespace overwrite: "@!Foo" => "@Foo"
            valueText = StringUtils.stripStart(valueText, "!");

            // normalize null value
            if(valueText.equals("~")) {
                valueText = "";
            }

            pair.add(Pair.create(valueText, keyText));
        }

        return pair;
    }

    /**
     * Replaces parameters and relative replaces strings
     *
     * "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
     * "%kernel.root_dir%" => "/app/../src/vendor/bundle/Resources/views"
     */
    @NotNull
    public static Collection<Pair<String, String>> getTwigPathFromYamlConfigResolved(@NotNull YAMLFile yamlFile) {
        VirtualFile baseDir = yamlFile.getProject().getBaseDir();

        Collection<Pair<String, String>> paths = new ArrayList<>();

        for (Pair<String, String> pair : getTwigPathFromYamlConfig(yamlFile)) {
            String second = pair.getSecond();

            if(second.startsWith("%kernel.root_dir%")) {
                // %kernel.root_dir%/../app
                // %kernel.root_dir%/foo
                for (VirtualFile appDir : FilesystemUtil.getAppDirectories(yamlFile.getProject())) {
                    String path = StringUtils.stripStart(second.substring("%kernel.root_dir%".length()), "/");

                    VirtualFile relativeFile = VfsUtil.findRelativeFile(appDir, path.split("/"));
                    if(relativeFile != null) {
                        String relativePath = VfsUtil.getRelativePath(relativeFile, baseDir, '/');
                        if(relativePath != null) {
                            paths.add(Pair.create(pair.getFirst(), relativePath));
                        }
                    }
                }
            } else if(second.startsWith("%kernel.project_dir%")) {
                // '%kernel.root_dir%/test'
                String path = StringUtils.stripStart(second.substring("%kernel.project_dir%".length()), "/");

                VirtualFile relativeFile = VfsUtil.findRelativeFile(yamlFile.getProject().getBaseDir(), path.split("/"));
                if(relativeFile != null) {
                    String relativePath = VfsUtil.getRelativePath(relativeFile, baseDir, '/');
                    if(relativePath != null) {
                        paths.add(Pair.create(pair.getFirst(), relativePath));
                    }
                }
            }
        }

         return paths;
    }

    /**
     * Collects Twig globals in given yaml configuration
     *
     * twig:
     *    globals:
     *       ga_tracking: '%ga_tracking%'
     *       user_management: '@AppBundle\Service\UserManagement'
     */
    @NotNull
    public static Map<String, String> getTwigGlobalsFromYamlConfig(@NotNull YAMLFile yamlFile) {
        YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile(yamlFile, "twig", "globals");
        if(yamlKeyValue == null) {
            return Collections.emptyMap();
        }

        YAMLValue value = yamlKeyValue.getValue();
        if(!(value instanceof YAMLMapping)) {
            return Collections.emptyMap();
        }

        Map<String, String> pair = new HashMap<>();

        for (YAMLPsiElement element : value.getYAMLElements()) {
            if(!(element instanceof YAMLKeyValue)) {
                continue;
            }

            String keyText = ((YAMLKeyValue) element).getKeyText();
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            String valueText = ((YAMLKeyValue) element).getValueText();
            if(StringUtils.isBlank(valueText)) {
                continue;
            }

            pair.put(keyText, valueText);
        }

        return pair;
    }

    /**
     * {% trans with {'%name%': 'Fabien'} from "aa" %}
     */
    @Nullable
    public static String getDomainFromTranslationTag(@NotNull TwigCompositeElement twigCompositeElement) {
        // getChildren fix
        PsiElement firstChild = twigCompositeElement.getFirstChild();
        if(firstChild == null) {
            return null;
        }

        // with from identifier and get text value
        PsiElement childrenOfType = PsiElementUtils.getNextSiblingOfType(firstChild, getTranslationTokenTagFromPattern());
        if(childrenOfType == null) {
            return null;
        }

        String text = childrenOfType.getText();
        if(StringUtils.isBlank(text)) {
            return null;
        }

        return text;
    }

    /**
     *  {% extends 'foobar.html.twig' %}
     *
     *  {{ block('foo<caret>bar') }}
     *  {% block 'foo<caret>bar' %}
     *  {% block foo<caret>bar %}
     */
    @NotNull
    public static Collection<PsiElement> getBlocksByImplementations(@NotNull PsiElement blockPsiName) {
        PsiFile psiFile = blockPsiName.getContainingFile();
        if(psiFile == null) {
            return Collections.emptyList();
        }

        Collection<PsiFile> twigChild = TwigUtil.getTemplatesExtendingFile(psiFile);
        if(twigChild.size() == 0) {
            return Collections.emptyList();
        }

        String blockName = blockPsiName.getText();
        if(StringUtils.isBlank(blockName)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> blockTargets = new ArrayList<>();
        for(PsiFile psiFile1: twigChild) {
            blockTargets.addAll(Arrays.asList(PsiTreeUtil.collectElements(psiFile1, psiElement1 ->
                (TwigHelper.getBlockTagPattern().accepts(psiElement1) || TwigHelper.getPrintBlockFunctionPattern("block").accepts(psiElement1)) && blockName.equals(psiElement1.getText())))
            );
        }

        return blockTargets;
    }

    /**
     * Twig template visitor, which scan given TwigPath for template names
     *
     * "@Foo/foo.html.twig"
     * "foo.html.twig"
     * "FooBundle:foo:foo.html.twig"
     */
    private static class MyLimitedVirtualFileVisitor extends VirtualFileVisitor {
        @NotNull
        private final TwigPath twigPath;

        @NotNull
        private final Project project;

        @NotNull
        private Map<String, VirtualFile> results = new HashMap<>();

        private boolean withPhp = false;
        private boolean withTwig = true;
        private int childrenAllowToVisit = 1000;

        @NotNull
        private Set<String> workedOn = new HashSet<>();

        MyLimitedVirtualFileVisitor(@NotNull Project project, @NotNull TwigPath twigPath, boolean withPhp, boolean withTwig, int maxDepth, int maxDirs) {
            super(VirtualFileVisitor.limit(maxDepth));

            this.project = project;
            this.twigPath = twigPath;
            this.withPhp = withPhp;
            this.withTwig = withTwig;
            this.childrenAllowToVisit = maxDirs;
        }

        @Override
        public boolean visitFile(@NotNull VirtualFile virtualFile) {
            // per path directory limit
            if (virtualFile.isDirectory() && childrenAllowToVisit-- <= 0) {
                return false;
            }

            processFile(virtualFile);

            return super.visitFile(virtualFile);
        }

        private void processFile(VirtualFile virtualFile) {
            // @TODO make file types more dynamically like eg js
            if(!this.isProcessable(virtualFile)) {
                return;
            }

            // prevent process double file if processCollection and ContentIterator is used in one instance
            String filePath = virtualFile.getPath();
            if(workedOn.contains(filePath)) {
                return;
            }

            workedOn.add(filePath);

            String templateName = TwigHelper.getTemplateNameForTwigPath(project, twigPath, virtualFile);
            if(templateName != null) {
                results.put(templateName, virtualFile);
            }
        }

        private boolean isProcessable(VirtualFile virtualFile) {
            if(virtualFile.isDirectory()) {
                return false;
            }

            if(withTwig && virtualFile.getFileType() instanceof TwigFileType) {
                return true;
            }

            if(withPhp && virtualFile.getFileType() instanceof PhpFileType) {
                return true;
            }

            return false;
        }

        @NotNull
        public Map<String, VirtualFile> getResults() {
            return results;
        }
    }

    /**
     * trans({
     *  %some%': "button.reserve"|trans,
     *  %vars%': "button.reserve"|trans({}, '<caret>')
     * })
     */
    private static class MyBeforeColonAndInsideLiteralPatternCondition extends PatternCondition<PsiElement> {
        MyBeforeColonAndInsideLiteralPatternCondition() {
            super("BeforeColonAndInsideLiteralPattern");
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            IElementType elementType = psiElement.getNode().getElementType();
            return
                elementType != TwigTokenTypes.LBRACE_CURL &&
                elementType != TwigTokenTypes.RBRACE_CURL &&
                elementType != TwigTokenTypes.COLON;
        }
    }
}
