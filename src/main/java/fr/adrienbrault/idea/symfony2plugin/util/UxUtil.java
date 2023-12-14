package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.ConfigIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
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

        for (String key : IndexUtil.getAllKeysForProject(ConfigStubIndex.KEY, project)) {
            for (ConfigIndex value : FileBasedIndex.getInstance().getValues(ConfigStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                if ("twig_component_defaults".equals(value.getName())) {
                    for (Map.Entry<String, TreeMap<String, String>> entry : value.getConfigs().entrySet()) {
                        String templateDirectory = entry.getValue().get("template_directory");
                        if (templateDirectory == null) {
                            continue;
                        }

                        namespaces.add(new TwigComponentNamespace(entry.getKey(), templateDirectory, entry.getValue().get("name_prefix")));
                    }
                }
            }
        }

        // add default if not presented
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
                String name = fqn.substring(componentNamespace.length());
                if (name.isBlank()) {
                    continue;
                }

                String fileName = name.replace("\\", "/");

                // name_prefix: Pizza
                // App\Pizza\Components\Button\Primary => Pizza:Button:Primary
                if (twigComponentNamespace.namePrefix() != null) {
                    fileName = twigComponentNamespace.namePrefix().replace(":", "/") + "/" + fileName;
                }

                templates.add(StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/" + fileName + ".html.twig");
            } else {
                for (String name : names) {
                    String fileName = name.replace(":", "/");

                    // name_prefix: Pizza
                    // App\Pizza\Components\Button\Primary => Pizza:Button:Primary
                    if (twigComponentNamespace.namePrefix() != null) {
                        fileName = twigComponentNamespace.namePrefix().replace(":", "/") + "/" + fileName;
                    }

                    templates.add(StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/" + fileName + ".html.twig");
                }
            }
        }

        return templates;
    }

    public static Set<String> getTwigComponentNames(@NotNull Project project) {
        // @TODO filter TwigComponentType.TWIG_COMPONENT
        return getAllComponentNames(project).stream().map(TwigComponent::name).collect(Collectors.toSet());
    }

    @NotNull
    private static Set<String> getAnonymousTemplateDirectories(@NotNull Project project) {
        Set<String> list = new HashSet<>();

        for (String key : IndexUtil.getAllKeysForProject(ConfigStubIndex.KEY, project)) {
            for (ConfigIndex value : FileBasedIndex.getInstance().getValues(ConfigStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                if ("anonymous_template_directory".equals(value.getName())) {
                    list.addAll(value.getValues());
                }
            }
        }

        // provide default, is there was noting found
        if (list.isEmpty()) {
            list.add("components/");
        }

        return list;
    }

    public static Collection<TwigComponent> getAllComponentNames(@NotNull Project project) {
        Map<String, TwigComponent> names = new HashMap<>();

        for (String key : IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)) {
            for (UxComponent value : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                for (TwigComponentNamespace namespace : getNamespaces(project)) {
                    String namespace1 = "\\" + StringUtils.strip(namespace.namespace(), "\\") + "\\";

                    if (value.phpClass().startsWith(namespace1)) {

                        String name;
                        if (value.name() != null) {
                            name = value.name();
                        } else {
                            name = value.phpClass().substring(namespace1.length());
                        }

                        if (!name.isBlank()) {
                            if (namespace.namePrefix() != null) {
                                name = namespace.namePrefix() + ":" + name;
                            }

                            String uxName = name.replace("\\", ":");
                            names.put(uxName, new TwigComponent(uxName, value.phpClass(), namespace));
                        }
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

                VirtualFile finalRelativeFile = relativeFile;
                VfsUtil.visitChildrenRecursively(relativeFile, new VirtualFileVisitor<>() {
                    @Override
                    public boolean visitFile(@NotNull VirtualFile file) {
                        if (file.isDirectory()) {
                            return super.visitFile(file);
                        }

                        if (!"twig".equals(file.getExtension())) {
                            return super.visitFile(file);
                        }

                        String relativePath = VfsUtil.getRelativePath(file, finalRelativeFile, '/');
                        if (relativePath == null) {
                            return super.visitFile(file);
                        }

                        // replace ".html.twig" maybe also other formats
                        String replace = relativePath.replace("/", ":").replaceAll("((\\.html|\\.json)*\\.twig)$", "");
                        if (!names.containsKey(replace)) {
                            names.put(replace, new TwigComponent(replace, null, null));
                        }

                        return super.visitFile(file);
                    }
                });
            }
        }

        return names.values();
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
        Collection<VirtualFile> virtualFiles = new HashSet<>();

        for (TwigComponent entry: getAllComponentNames(project)) {
            if (!entry.name().equals(component)) {
                continue;
            }

            if (entry.twigComponentNamespace != null) {
                if ((entry.twigComponentNamespace.namePrefix() == null || component.startsWith(entry.twigComponentNamespace.namePrefix() + ":"))) {
                    String strip = StringUtils.strip(entry.twigComponentNamespace.templateDirectory(), "/");
                    String template = strip + "/" + component.replace(":", "/") + ".html.twig";
                    virtualFiles.addAll(TwigUtil.getTemplateFiles(project, template));
                }
            } else {
                for (String directory : getAnonymousTemplateDirectories(project)) {
                    String templateName = StringUtils.strip(directory, "/") + "/" + component.replace(":", "/") + ".html.twig";
                    virtualFiles.addAll(TwigUtil.getTemplateFiles(project, templateName));
                }
            }
        }

        return PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles);
    }

    @NotNull
    public static Collection<PhpClass> getComponentClassesForTemplateFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        for (String template : TwigUtil.getTemplateNamesForFile(psiFile)) {
            // attribute: template: "foo.html.twig"
            Collection<PhpClass> phpClassesTemplateMatch = new HashSet<>();
            for (UxComponent uxComponent : getComponentsWithTemplates(project)) {
                if (template.equals(uxComponent.template()))  {
                    PhpClass classInterface = PhpElementsUtil.getClassInterface(project, uxComponent.phpClass());
                    if (classInterface != null) {
                        phpClassesTemplateMatch.add(classInterface);
                    }
                }
            }

            if (!phpClassesTemplateMatch.isEmpty()) {
                phpClasses.addAll(phpClassesTemplateMatch);
                break;
            }

            for (TwigComponentNamespace twigComponentNamespace : getNamespaces(project)) {
                String templateDirectory = StringUtils.stripEnd(twigComponentNamespace.templateDirectory(), "/") + "/";
                if (template.startsWith(templateDirectory)) {
                    String name = template.substring(templateDirectory.length());

                    if (twigComponentNamespace.namePrefix() != null) {
                        String prefix = twigComponentNamespace.namePrefix().replace(":", "/") + "/";
                        if (!name.startsWith(prefix)) {
                            continue;
                        }

                        name = name.substring(prefix.length());
                    }

                    String s = name.replace("/", "\\");
                    String phpClassFqn = "\\" + (StringUtils.stripEnd(twigComponentNamespace.namespace(), "\\") + "\\" + s)
                        .replace(".html.twig", "");

                    PhpClass classInterface = PhpElementsUtil.getClassInterface(project, phpClassFqn);
                    if (classInterface != null) {
                        phpClasses.add(classInterface);
                    }
                }
            }
        }

        return phpClasses;
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
