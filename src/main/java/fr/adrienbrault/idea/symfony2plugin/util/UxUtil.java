package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
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
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Key<CachedValue<Collection<UxComponent>>> SYMFONY_UX_COMPONENTS = new Key<>("SYMFONY_UX_COMPONENTS");

    private static final Key<CachedValue<Set<String>>> TWIG_COMPONENTS = new Key<>("SYMFONY_TWIG_COMPONENTS");

    public static void visitComponents(@NotNull PhpFile phpFile, @NotNull Consumer<TwigComponent> consumer) {
        for (PhpNamedElement topLevelElement : phpFile.getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                visitComponents(clazz, consumer);
            }
        }
    }

    public static void visitComponents(@NotNull PhpClass clazz, @NotNull Consumer<TwigComponent> consumer) {
        for (Map.Entry<TwigComponentType, String> entry : COMPONENTS.entrySet()) {
            for (PhpAttribute attribute : clazz.getAttributes(entry.getValue())) {
                String name = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
                if (name == null) {
                    name = clazz.getName();
                }

                String template = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 1, "template");
                if (template != null && template.contains(":")) {
                    template = template.replaceAll(":", "/");
                    if (!template.endsWith(".twig")) {
                        template += ".html.twig";
                    }

                    if (!template.startsWith("components/")) {
                        template = "components/" + template;
                    }
                }

                consumer.accept(new TwigComponent(name, clazz, template, entry.getKey()));
            }
        }
    }

    public static Set<String> getTwigComponentNames(@NotNull Project project) {
        Set<String> names = new HashSet<>();

        for (String key : FileIndexCaches.getIndexKeysCache(project, TWIG_COMPONENTS, UxTemplateStubIndex.KEY)) {
            names.addAll(FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, key, GlobalSearchScope.allScope(project))
                .stream()
                .filter(uxComponent -> uxComponent.type() == TwigComponentType.TWIG_COMPONENT)
                .map(UxComponent::name)
                .collect(Collectors.toSet())
            );
        }

        return names;
    }

    public static Set<String> getAllComponentNames(@NotNull Project project) {
        return FileIndexCaches.getIndexKeysCache(project, TWIG_COMPONENTS, UxTemplateStubIndex.KEY);
    }

    public static Set<PhpClass> getTwigComponentNameTargets(@NotNull Project project, @NotNull String name) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (UxComponent fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, name, GlobalSearchScope.allScope(project))) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, fqn.phpClass());
            if (classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    @NotNull
    private static Collection<UxComponent> getAllUxComponents(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_UX_COMPONENTS,
            () -> {
                Collection<UxComponent> uxComponents = new ArrayList<>();

                for (String key : IndexUtil.getAllKeysForProject(UxTemplateStubIndex.KEY, project)) {
                    uxComponents.addAll(FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, key, GlobalSearchScope.allScope(project)));
                }

                return CachedValueProvider.Result.create(uxComponents, FileIndexCaches.getModificationTrackerForIndexId(project, UxTemplateStubIndex.KEY));
            },
            false
        );
    }

    public static Collection<PsiFile> getComponentTemplates(@NotNull Project project, @NotNull String component) {
        Collection<PsiFile> psiFiles = new HashSet<>();

        for (UxComponent allUxComponent : getAllUxComponents(project).stream().filter(uxComponent -> component.equals(uxComponent.name())).toList()) {
            String template = allUxComponent.template();

            if (template != null) {
                psiFiles.addAll(TwigUtil.getTemplatePsiElements(project, template));
            } else {
                psiFiles.addAll(TwigUtil.getTemplatePsiElements(project, "components/" + allUxComponent.name() + ".html.twig"));
            }
        }

        return psiFiles;
    }

    @NotNull
    public static Collection<PhpClass> getComponentClassesForTemplateFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        Collection<UxComponent> uxAllComponents = null;

        for (String template : TwigUtil.getTemplateNamesForFile(psiFile)) {
            if (uxAllComponents == null) {
                uxAllComponents = getAllUxComponents(project);
            }

            // "template:"
            for (UxComponent uxComponent : uxAllComponents.stream().filter(uxComponent -> template.equals(uxComponent.template())).toList()) {
                phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, uxComponent.phpClass()));
            }

            // no template given
            Matcher matcher = Pattern.compile("^components/([\\w-]+)\\.html\\.twig$").matcher(template);
            if (matcher.find()) {
                String name = matcher.group(1);
                for (UxComponent uxComponent : uxAllComponents.stream().filter(uxComponent -> name.equals(uxComponent.name())).toList()) {
                    phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, uxComponent.phpClass()));
                }
            }
        }

        return phpClasses;
    }

    public static Set<PhpClass> getTwigComponentAllTargets(@NotNull Project project) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (String twigComponentName : getAllComponentNames(project)) {
            for (UxComponent fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, twigComponentName, GlobalSearchScope.allScope(project))) {
                PhpClass classInterface = PhpElementsUtil.getClassInterface(project, fqn.phpClass());
                if (classInterface != null) {
                    phpClasses.add(classInterface);
                }
            }
        }

        return phpClasses;
    }


    public static Collection<LookupElement> getComponentLookupElements(@NotNull Project project) {
        Map<String, String> components = new HashMap<>();

        for (String twigComponentName : getAllComponentNames(project)) {
            for (UxComponent fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, twigComponentName, GlobalSearchScope.allScope(project))) {
                components.put(twigComponentName, fqn.phpClass());
            }
        }

        return components.entrySet()
            .stream()
            .map(entry ->
                LookupElementBuilder.create(entry.getKey())
                    .withIcon(Symfony2Icons.SYMFONY)
                    .withTypeText(StringUtils.stripStart(entry.getValue(), "\\"))
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
        Collection<@NotNull PhpAttribute> attributes = phpAttributesOwner.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE);
        if (attributes.size() == 0) {
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

    public enum TwigComponentType {
        LIVE_COMPONENT,
        TWIG_COMPONENT,
    }

    public record TwigComponent(String name, PhpClass phpClass, String template, TwigComponentType type) {}
}
