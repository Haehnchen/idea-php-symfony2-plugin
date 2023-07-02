package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import kotlin.Pair;
import kotlin.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxUtil {
    private static final String AS_TWIG_COMPONENT = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";
    private static final String ATTRIBUTE_EXPOSE_IN_TEMPLATE = "\\Symfony\\UX\\TwigComponent\\Attribute\\ExposeInTemplate";

    private static final Key<CachedValue<Set<String>>> TWIG_COMPONENTS = new Key<>("SYMFONY_TWIG_COMPONENTS");

    public static void visitAsTwigComponent(@NotNull PhpFile phpFile, @NotNull Consumer<Triple<String, PhpClass, String>> consumer) {
        for (PhpNamedElement topLevelElement : phpFile.getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                for (PhpAttribute attribute : clazz.getAttributes(AS_TWIG_COMPONENT)) {
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

                    consumer.accept(new Triple<>(name, clazz, template));
                }
            }
        }
    }

    public static Set<String> getTwigComponentNames(@NotNull Project project) {
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
    public static Collection<PhpClass> getComponentClassesForTemplateFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        for (String template : TwigUtil.getTemplateNamesForFile(psiFile)) {
            // @TODO: provide support for template resolve on attribute
            Matcher matcher = Pattern.compile("^components/([\\w-]+)\\.html\\.twig$").matcher(template);
            if (matcher.find()) {
                String group = matcher.group(1);
                phpClasses.addAll(UxUtil.getTwigComponentNameTargets(project, group));
            }
        }

        return phpClasses;
    }

    public static Set<PhpClass> getTwigComponentAllTargets(@NotNull Project project) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (String twigComponentName : getTwigComponentNames(project)) {
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

        for (String twigComponentName : getTwigComponentNames(project)) {
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

            if (field.getModifier().isPrivate() && field.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).size() > 0) {
                for (String name : getExposeName(field)) {
                    consumer.accept(new Pair<>(name, field));
                }
            }
        }

        for (Method method : phpClass.getMethods()) {
            if (method.getAccess().isPublic() && method.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).size() > 0) {
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
}
