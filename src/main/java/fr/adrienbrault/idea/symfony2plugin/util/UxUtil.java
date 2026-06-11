package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.ConfigIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.UxComponentTemplateFinderParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.dict.TwigComponentNamespace;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxUtil {
    public static final Map<TwigComponentType, String> COMPONENTS = new HashMap<>() {{
        put(TwigComponentType.TWIG_COMPONENT, AS_TWIG_COMPONENT);
        put(TwigComponentType.LIVE_COMPONENT, AS_LIVE_COMPONENT);
    }};
    private static final String AS_TWIG_COMPONENT = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";
    private static final String AS_LIVE_COMPONENT = "\\Symfony\\UX\\LiveComponent\\Attribute\\AsLiveComponent";

    private static final String ATTRIBUTE_EXPOSE_IN_TEMPLATE = "\\Symfony\\UX\\TwigComponent\\Attribute\\ExposeInTemplate";

    private static final Key<CachedValue<Collection<TwigComponentNamespace>>> TWIG_COMPONENTS_NAMESPACES = new Key<>("SYMFONY_TWIG_COMPONENTS_NAMESPACES");
    private static final Key<CachedValue<Collection<TwigComponent>>> NAMESPACED_ANONYMOUS_COMPONENTS = new Key<>("SYMFONY_UX_NAMESPACED_ANONYMOUS_COMPONENTS");
    private static final Key<CachedValue<Collection<String>>> COMPONENT_CLASS_FQNS_FOR_TEMPLATE_FILE = new Key<>("SYMFONY_UX_COMPONENT_CLASS_FQNS_FOR_TEMPLATE_FILE");

    public static Collection<TwigComponentNamespace> getNamespaces(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TWIG_COMPONENTS_NAMESPACES,
            () -> CachedValueProvider.Result.create(getNamespacesInner(project), FileIndexCaches.getModificationTrackerForIndexId(project, ConfigStubIndex.KEY)),
            false
        );
    }
    private static Collection<TwigComponentNamespace> getNamespacesInner(@NotNull Project project) {
        Collection<TwigComponentNamespace> namespaces = new ArrayList<>();

        // Symfony UX supports short-form defaults:
        //
        // twig_component:
        //   defaults:
        //     App\Twig\Components\: components/
        //
        // and long-form defaults:
        //
        // twig_component:
        //   defaults:
        //     App\Pizza\Components\:
        //       template_directory: components/pizza
        //       name_prefix: Pizza
        //
        // ConfigStubIndex normalizes both forms into "template_directory" and
        // "name_prefix" entries while preserving the declaration order that
        // Symfony UX uses for first-matching namespace resolution.
        for (String key : IndexUtil.getAllKeysForProject(ConfigStubIndex.KEY, project)) {
            for (ConfigIndex value : FileBasedIndex.getInstance().getValues(ConfigStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                if ("twig_component_defaults".equals(value.getName())) {
                    for (Map.Entry<String, LinkedHashMap<String, String>> entry : value.getConfigs().entrySet()) {
                        String templateDirectory = entry.getValue().get("template_directory");
                        if (templateDirectory == null) {
                            continue;
                        }

                        namespaces.add(new TwigComponentNamespace(entry.getKey(), templateDirectory, entry.getValue().get("name_prefix")));
                    }
                }
            }
        }

        // Symfony Flex projects commonly omit explicit defaults because the
        // runtime default maps App\Twig\Components\Alert to the public
        // component name "Alert" and template "components/Alert.html.twig".
        if (namespaces.stream().noneMatch(n -> "App\\Twig\\Components".equals(StringUtils.strip(n.namespace(), "\\")))) {
            namespaces.add(new TwigComponentNamespace("App\\Twig\\Components\\", "components/", null));
        }

        return namespaces;
    }

    public static void visitComponentsForIndex(@NotNull PhpFile phpFile, @NotNull Consumer<TwigComponentIndex> consumer) {
        for (PhpNamedElement topLevelElement : phpFile.getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                for (Map.Entry<TwigComponentType, String> entry : COMPONENTS.entrySet()) {
                    for (PhpAttribute attribute : clazz.getAttributes(entry.getValue())) {
                        String name = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
                        String template = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 1, "template");

                        if (template != null && template.contains(":")) {
                            template = template.replace(":", "/");
                            if (!template.endsWith(".twig")) {
                                template += ".html.twig";
                            }
                        }

                        consumer.accept(new TwigComponentIndex(name, clazz, template, entry.getKey()));
                    }
                }
            }
        }
    }

    public static Collection<String> getComponentTemplatesForPhpClass(@NotNull PhpClass phpClass) {
        Set<String> templates = new HashSet<>();

        Set<String> names = new HashSet<>();

        for (UxComponent value : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, phpClass.getFQN(), GlobalSearchScope.allScope(phpClass.getProject()))) {
            if (value.template() != null) {
                templates.add(value.template());
            }

            if (value.name() != null) {
                names.add(value.name());
            }
        }

        if (!templates.isEmpty()) {
            return templates;
        }

        String fqn = phpClass.getFQN();
        for (TwigComponentNamespace twigComponentNamespace : getNamespaces(phpClass.getProject())) {
            String componentNamespace = "\\" + StringUtils.strip(twigComponentNamespace.namespace(), "\\") + "\\";
            if (!fqn.startsWith(componentNamespace)) {
                continue;
            }


            if (names.isEmpty()) {
                String name = fqn.substring(componentNamespace.length()).replace("\\", "/");
                if (name.isBlank()) {
                    break;
                }

                templates.add(StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/" + name + ".html.twig");
            } else {
                for (String name : names) {
                    String componentName = removeNamePrefix(name, twigComponentNamespace.namePrefix());
                    if (componentName == null) {
                        continue;
                    }

                    templates.add(StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/" + componentName.replace(":", "/") + ".html.twig");
                }
            }

            break;
        }

        return templates;
    }

    public static Set<String> getTwigComponentNames(@NotNull Project project) {
        // @TODO filter TwigComponentType.TWIG_COMPONENT
        return getAllComponentNames(project).stream().map(TwigComponent::name).collect(Collectors.toSet());
    }

    @Nullable
    public static String resolveTwigComponentName(@NotNull Project project, @NotNull String name) {
        Set<String> names = getTwigComponentNames(project);
        return names.contains(name) ? name : null;
    }

    public static boolean hasTwigComponentName(@NotNull Project project, @NotNull String name) {
        return resolveTwigComponentName(project, name) != null;
    }

    @NotNull
    private static Set<String> getAnonymousTemplateDirectories(@NotNull Project project) {
        Set<String> list = new LinkedHashSet<>();

        // Source A: YAML config via index (twig_component.anonymous_template_directory)
        for (String key : IndexUtil.getAllKeysForProject(ConfigStubIndex.KEY, project)) {
            for (ConfigIndex value : FileBasedIndex.getInstance().getValues(ConfigStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                if ("anonymous_template_directory".equals(value.getName())) {
                    list.addAll(value.getValues());
                }
            }
        }

        // Source B: compiled container XML (ux.twig_component.component_template_finder second argument)
        UxComponentTemplateFinderParser containerParser = ServiceXmlParserFactory.getInstance(project, UxComponentTemplateFinderParser.class);
        if (containerParser != null) {
            list.addAll(containerParser.getTemplateDirectories());
        }

        // provide default if nothing was found
        if (list.isEmpty()) {
            list.add("components/");
        }

        return list;
    }

    @Nullable
    private static String removeNamePrefix(@NotNull String componentName, @Nullable String namePrefix) {
        if (StringUtils.isBlank(namePrefix)) {
            return componentName;
        }

        String prefix = namePrefix + ":";
        return componentName.startsWith(prefix) ? componentName.substring(prefix.length()) : null;
    }

    @NotNull
    private static String addNamePrefix(@NotNull String componentName, @Nullable String namePrefix) {
        return StringUtils.isBlank(namePrefix) ? componentName : namePrefix + ":" + componentName;
    }

    public static Collection<TwigComponent> getAllComponentNames(@NotNull Project project) {
        Map<String, TwigComponent> names = new HashMap<>();

        for (String key : IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)) {
            for (UxComponent value : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                for (TwigComponentNamespace namespace : getNamespaces(project)) {
                    String namespace1 = "\\" + StringUtils.strip(namespace.namespace(), "\\") + "\\";

                    if (value.phpClass().startsWith(namespace1)) {
                        String name = value.name() != null
                            ? value.name()
                            : addNamePrefix(value.phpClass().substring(namespace1.length()).replace("\\", ":"), namespace.namePrefix());

                        if (!name.isBlank()) {
                            names.put(name, new TwigComponent(name, value.phpClass(), namespace));
                        }

                        break;
                    }
                }
            }
        }

        for (String valueValue : getAnonymousTemplateDirectories(project)) {
            String namespace = null;
            if (valueValue.startsWith("@")) {
                namespace = StringUtils.stripEnd(valueValue.replace("\\", "/"), "/");
                int i = namespace.indexOf("/");
                if (i > 0) {
                    namespace = namespace.substring(0, i);
                }

                // strip "@"
                namespace = namespace.substring(1);
            }

            for (TwigPath twigPath : TwigUtil.getTwigNamespaces(project)) {
                VirtualFile relativeFile = null;

                if (namespace == null && twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == TwigUtil.NamespaceType.ADD_PATH) {
                    VirtualFile directory = twigPath.getDirectory(project);
                    relativeFile = VfsUtil.findRelativeFile(directory, StringUtils.strip(valueValue.replace("\\", "/"), "/").split("/"));
                } else if (namespace != null && twigPath.getNamespaceType() == TwigUtil.NamespaceType.ADD_PATH && namespace.equals(twigPath.getNamespace())) {
                    // @namespace resolve
                    VirtualFile directory = twigPath.getDirectory(project);
                    String[] split2 = StringUtils.strip(valueValue.replace("\\", "/"), "/").split("/");
                    String[] split = Arrays.copyOfRange(split2, 1, split2.length);
                    relativeFile = VfsUtil.findRelativeFile(directory, split);
                }

                if (relativeFile == null) {
                    continue;
                }

                visitAnonymousTemplateComponents(relativeFile, null, names);
            }
        }

        for (TwigComponent component : getNamespacedAnonymousComponents(project)) {
            names.putIfAbsent(component.name(), component);
        }

        return names.values();
    }

    @NotNull
    private static Collection<TwigComponent> getNamespacedAnonymousComponents(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            NAMESPACED_ANONYMOUS_COMPONENTS,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableCollection(getNamespacedAnonymousComponentsInner(project)),
                VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
            ),
            false
        );
    }

    @NotNull
    private static Collection<TwigComponent> getNamespacedAnonymousComponentsInner(@NotNull Project project) {
        Map<String, TwigComponent> names = new HashMap<>();

        for (TwigPath twigPath : TwigUtil.getTwigNamespaces(project)) {
            if (!twigPath.isEnabled() || twigPath.isGlobalNamespace() || twigPath.getNamespaceType() != TwigUtil.NamespaceType.ADD_PATH) {
                continue;
            }

            VirtualFile directory = twigPath.getDirectory(project);
            VirtualFile componentsDirectory = directory != null ? VfsUtil.findRelativeFile(directory, "components") : null;
            if (componentsDirectory == null) {
                continue;
            }

            visitAnonymousTemplateComponents(componentsDirectory, twigPath.getNamespace(), names);
        }

        return names.values();
    }

    private static void visitAnonymousTemplateComponents(@NotNull VirtualFile rootDirectory, @Nullable String namePrefix, @NotNull Map<String, TwigComponent> names) {
        VfsUtil.visitChildrenRecursively(rootDirectory, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                    return super.visitFile(file);
                }

                if (!"twig".equals(file.getExtension())) {
                    return super.visitFile(file);
                }

                String relativePath = VfsUtil.getRelativePath(file, rootDirectory, '/');
                if (relativePath == null) {
                    return super.visitFile(file);
                }

                String componentName = getAnonymousComponentNameFromRelativePath(relativePath);
                if (componentName != null) {
                    String name = StringUtils.isBlank(namePrefix) ? componentName : namePrefix + ":" + componentName;
                    names.putIfAbsent(name, new TwigComponent(name, null, null));
                }

                return super.visitFile(file);
            }
        });
    }

    @NotNull
    public static Set<PhpClass> getTwigComponentPhpClasses(@NotNull Project project, @NotNull String component) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (TwigComponent entry : getAllComponentNames(project)) {
            if (!entry.name().equals(component) || entry.phpClass() == null) {
                continue;
            }

            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, entry.phpClass());
            if (classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    public static Collection<PsiFile> getComponentTemplates(@NotNull Project project, @NotNull String component) {
        Collection<VirtualFile> virtualFiles = new LinkedHashSet<>();

        for (TwigComponent entry: getAllComponentNames(project)) {
            if (!entry.name().equals(component)) {
                continue;
            }

            if (entry.twigComponentNamespace != null) {
                String componentTemplateName = removeNamePrefix(component, entry.twigComponentNamespace.namePrefix());
                if (componentTemplateName != null) {
                    String strip = StringUtils.strip(entry.twigComponentNamespace.templateDirectory(), "/");
                    String template = strip + "/" + componentTemplateName.replace(":", "/") + ".html.twig";
                    addTemplateFilesWithFallback(project, template, virtualFiles);
                }
            } else {
                addAnonymousComponentTemplateFiles(project, component, virtualFiles);
            }
        }

        return PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles);
    }

    @Nullable
    private static String getAnonymousComponentNameFromRelativePath(@NotNull String relativePath) {
        // Normalize Twig template names to component names (e.g. "Nav/index.html.twig" -> "Nav:index")
        String normalized = relativePath.replace("/", ":").replaceAll("(\\.html)?\\.twig$", "");
        if (normalized.isBlank()) {
            return null;
        }

        // Symfony UX 2.32:
        // components/Nav/index.html.twig => "Nav"
        // components/Nav/Sub/index.html.twig => "Nav:Sub"
        if ("index".equals(normalized)) {
            return null;
        }

        if (normalized.endsWith(":index")) {
            return normalized.substring(0, normalized.length() - 6);
        }

        return normalized;
    }

    private static void addAnonymousComponentTemplateFiles(@NotNull Project project, @NotNull String component, @NotNull Collection<VirtualFile> virtualFiles) {
        String componentPath = component.replace(":", "/");
        for (String directory : getAnonymousTemplateDirectories(project)) {
            String templateName = StringUtils.strip(directory, "/") + "/" + componentPath + ".html.twig";
            if (addTemplateFilesWithFallback(project, templateName, virtualFiles)) {
                return;
            }

            // Symfony UX 2.32: components/Foo/index.html.twig => <twig:Foo>
            String indexTemplateName = StringUtils.strip(directory, "/") + "/" + componentPath + "/index.html.twig";
            if (addTemplateFilesWithFallback(project, indexTemplateName, virtualFiles)) {
                return;
            }
        }

        int i = component.indexOf(":");
        if (i <= 0 || i == component.length() - 1) {
            return;
        }

        String namespace = component.substring(0, i);
        String componentName = component.substring(i + 1).replace(":", "/");
        if (addTemplateFilesWithFallback(project, "@" + namespace + "/components/" + componentName + ".html.twig", virtualFiles)) {
            return;
        }

        addTemplateFilesWithFallback(project, "@" + namespace + "/components/" + componentName + "/index.html.twig", virtualFiles);
    }

    @NotNull
    public static Set<String> getTemplateComponentNames(@NotNull TwigFile twigFile) {
        Project project = twigFile.getProject();
        Set<String> names = new HashSet<>();

        VirtualFile currentFile = twigFile.getVirtualFile();
        if (currentFile == null) {
            return names;
        }

        for (TwigComponent component : getAllComponentNames(project)) {
            if (component.name().isBlank() || names.contains(component.name())) {
                continue;
            }

            for (PsiFile template : getComponentTemplates(project, component.name())) {
                VirtualFile virtualFile = template.getVirtualFile();
                if (virtualFile != null && virtualFile.equals(currentFile)) {
                    names.add(component.name());
                    break;
                }
            }
        }

        return names;
    }

    public static boolean hasComponentUsages(@NotNull TwigFile twigFile) {
        Project project = twigFile.getProject();
        Collection<String> componentNames = getTemplateComponentNames(twigFile);
        VirtualFile excludeFile = twigFile.getVirtualFile();

        Set<String> normalizedComponentNames = new HashSet<>();
        for (String componentName : componentNames) {
            String normalized = TwigComponentUsageStubIndex.normalizeComponentName(componentName);
            if (normalized != null) {
                normalizedComponentNames.add(normalized);
            }
        }

        if (normalizedComponentNames.isEmpty()) {
            return false;
        }

        for (String componentName : normalizedComponentNames) {
            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigComponentUsageStubIndex.KEY,
                componentName,
                GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
            );

            for (VirtualFile containingFile : containingFiles) {
                if (excludeFile == null || !excludeFile.equals(containingFile)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean addTemplateFilesWithFallback(@NotNull Project project, @NotNull String templateName, @NotNull Collection<VirtualFile> virtualFiles) {
        Collection<VirtualFile> files = TwigUtil.getTemplateFiles(project, templateName);
        if (!files.isEmpty()) {
            virtualFiles.addAll(files);
            return true;
        }

        VirtualFile baseDir = ProjectUtil.getProjectDir(project);
        if (baseDir == null) {
            return false;
        }

        VirtualFile templatesDir = VfsUtil.findRelativeFile(baseDir, "templates");
        if (templatesDir != null) {
            VirtualFile virtualFile = VfsUtil.findRelativeFile(templatesDir, templateName.split("/"));
            if (virtualFile != null) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }

        VirtualFile appViewsDir = VfsUtil.findRelativeFile(baseDir, "app", "Resources", "views");
        if (appViewsDir != null) {
            VirtualFile virtualFile = VfsUtil.findRelativeFile(appViewsDir, templateName.split("/"));
            if (virtualFile != null) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static Collection<PhpClass> getComponentClassesForTemplateFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        Collection<String> phpClassFqns = CachedValuesManager.getCachedValue(
            psiFile,
            COMPONENT_CLASS_FQNS_FOR_TEMPLATE_FILE,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableCollection(getComponentClassFqnsForTemplateFileInner(project, psiFile)),
                psiFile,
                FileIndexCaches.getModificationTrackerForIndexId(project, UxTemplateStubIndex.KEY),
                FileIndexCaches.getModificationTrackerForIndexId(project, ConfigStubIndex.KEY)
            )
        );

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String phpClassFqn : phpClassFqns) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, phpClassFqn);
            if (classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    @NotNull
    private static Collection<String> getComponentClassFqnsForTemplateFileInner(@NotNull Project project, @NotNull PsiFile psiFile) {
        Collection<String> phpClassFqns = new HashSet<>();

        for (String template : TwigUtil.getTemplateNamesForFile(psiFile)) {
            // attribute: template: "foo.html.twig"
            Collection<String> phpClassFqnsTemplateMatch = new HashSet<>();
            for (UxComponent uxComponent : getComponentsWithTemplates(project)) {
                if (template.equals(uxComponent.template()))  {
                    phpClassFqnsTemplateMatch.add(uxComponent.phpClass());
                }
            }

            if (!phpClassFqnsTemplateMatch.isEmpty()) {
                phpClassFqns.addAll(phpClassFqnsTemplateMatch);
                break;
            }

            for (TwigComponentNamespace twigComponentNamespace : getNamespaces(project)) {
                String templateDirectory = StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/";
                if (template.startsWith(templateDirectory)) {
                    String name = template.substring(templateDirectory.length());

                    String s = name.replace("/", "\\");
                    String phpClassFqn = "\\" + (StringUtils.stripEnd(twigComponentNamespace.namespace(), "\\") + "\\" + s)
                        .replace(".html.twig", "");

                    phpClassFqns.add(phpClassFqn);
                }
            }
        }

        return phpClassFqns;
    }

    public static Set<PhpClass> getTwigComponentAllTargets(@NotNull Project project) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (TwigComponent entry : getAllComponentNames(project)) {
            if (entry.phpClass() == null) {
                continue;
            }

            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, entry.phpClass());
            if (classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }


    public static Collection<LookupElement> getComponentLookupElements(@NotNull Project project) {
        Map<String, String> components = new HashMap<>();

        for (TwigComponent entry : getAllComponentNames(project)) {
            components.put(entry.name(), entry.phpClass());
        }

        return components.entrySet()
            .stream()
            .map(entry ->
                LookupElementBuilder.create(entry.getKey())
                    .withIcon(Symfony2Icons.SYMFONY)
                    .withTypeText(entry.getValue() != null ? StringUtils.stripStart(entry.getValue(), "\\") : "anonymous")
            )
            .collect(Collectors.toList());
    }

    public static void visitComponentVariables(@NotNull PhpClass phpClass, @NotNull Consumer<Pair<String, PhpNamedElement>> consumer) {
        for (Field field : phpClass.getFields()) {
            if (field.getModifier().isPublic()) {
                for (String name : getExposeName(field)) {
                    consumer.accept(new Pair<>(name, field));
                }
            }

            if (field.getModifier().isPrivate() && !field.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).isEmpty()) {
                for (String name : getExposeName(field)) {
                    consumer.accept(new Pair<>(name, field));
                }
            }
        }

        for (Method method : phpClass.getMethods()) {
            if (method.getAccess().isPublic() && !method.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).isEmpty()) {
                for (String name : getExposeName(method)) {
                    consumer.accept(new Pair<>(name, method));
                }
            }
        }
    }

    public static void visitComponentTemplateProps(@NotNull Project project, @NotNull String componentName, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        for (PsiFile componentTemplate : getComponentTemplates(project, componentName)) {
            if (componentTemplate instanceof TwigFile twigFile) {
                visitComponentTemplateProps(twigFile, consumer);
            }
        }
    }

    public static void visitComponentTemplateProps(@NotNull TwigFile twigFile, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        for (PsiElement element : twigFile.getChildren()) {
            if (element instanceof TwigCompositeElement && element.getNode().getElementType() == TwigElementTypes.TAG) {
                PsiElement firstChild1 = element.getFirstChild();
                if (firstChild1 == null) {
                    continue;
                }

                PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild1, TwigTokenTypes.TAG_NAME);
                if (tagName == null) {
                    continue;
                }

                ASTNode nextNonWhitespaceLeaf = FormatterUtil.getNextNonWhitespaceLeaf(tagName.getNode());
                if (nextNonWhitespaceLeaf == null || nextNonWhitespaceLeaf.getElementType() != TwigTokenTypes.IDENTIFIER) {
                    continue;
                }

                String text = nextNonWhitespaceLeaf.getText();
                if (!text.isBlank()) {
                    consumer.accept(new Pair<>(text, nextNonWhitespaceLeaf.getPsi()));
                }

                for (PsiElement commaPsi : PsiElementUtils.getChildrenOfTypeAsList(element, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.COMMA))) {
                    ASTNode propName = FormatterUtil.getNextNonWhitespaceLeaf(commaPsi.getNode());
                    if (propName == null || propName.getElementType() != TwigTokenTypes.IDENTIFIER) {
                        continue;
                    }

                    String propText = propName.getText();
                    if (!propText.isBlank()) {
                        consumer.accept(new Pair<>(propText, propName.getPsi()));
                    }
                }
            }
        }
    }


    private static Collection<String> getExposeName(@NotNull PhpAttributesOwner phpAttributesOwner) {
        Collection<String> names = new HashSet<>();

        // public state
        Collection<PhpAttribute> attributes = phpAttributesOwner.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE);
        if (attributes.isEmpty()) {
            String name = phpAttributesOwner.getName();

            if (phpAttributesOwner instanceof Method method) {
                names.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method));
            } else {
                names.add(name);
            }

            return names;
        }

        // attributes given
        for (PhpAttribute attribute : attributes) {
            String name = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "name");
            if (name != null && !name.isBlank()) {
                names.add(name);
                break;
            }

            if (phpAttributesOwner instanceof Method method) {
                // public function getActions(): array // available as `{{ actions }}`
                names.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method));
            } else {
                names.add(phpAttributesOwner.getName());
            }
        }

        return names;
    }

    private static Collection<UxComponent> getComponentsWithTemplates(@NotNull Project project) {
        return IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)
            .stream().flatMap(key -> FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, key, GlobalSearchScope.allScope(project)).stream())
            .filter(value -> value.template() != null)
            .collect(Collectors.toList());
    }

    public enum TwigComponentType {
        LIVE_COMPONENT,
        TWIG_COMPONENT,
    }

    public record TwigComponentIndex(@Nullable String name, @NotNull PhpClass phpClass, @Nullable String template, @NotNull TwigComponentType type) {}

    public record TwigComponent(@NotNull String name, @Nullable String phpClass, @Nullable TwigComponentNamespace twigComponentNamespace) {}
}
