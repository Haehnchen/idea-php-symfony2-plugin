package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxTemplateStubIndex;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxUtil {
    private static String AS_TWIG_COMPONENT = "\\Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent";

    private static final Key<CachedValue<Set<String>>> TWIG_COMPONENTS = new Key<>("SYMFONY_TWIG_COMPONENTS");

    public static void visitAsTwigComponent(@NotNull PhpFile phpFile, @NotNull Consumer<Pair<String, PhpClass>> consumer) {
        for (PhpNamedElement topLevelElement : phpFile.getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                for (PhpAttribute attribute : clazz.getAttributes(AS_TWIG_COMPONENT)) {
                    String name = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "name");
                    if (name == null) {
                        name = clazz.getName();
                    }

                    consumer.accept(new Pair<>(name, clazz));
                }
            }
        }
    }

    public static Set<String> getTwigComponentNames(@NotNull Project project) {
        return FileIndexCaches.getIndexKeysCache(project, TWIG_COMPONENTS, UxTemplateStubIndex.KEY);
    }

    public static Set<PhpClass> getTwigComponentNameTargets(@NotNull Project project, @NotNull String name) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (String fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, name, GlobalSearchScope.allScope(project))) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, fqn);
            if (classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    public static Set<PhpClass> getTwigComponentAllTargets(@NotNull Project project) {
        Set<PhpClass> phpClasses = new HashSet<>();

        for (String twigComponentName : getTwigComponentNames(project)) {
            for (String fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, twigComponentName, GlobalSearchScope.allScope(project))) {
                PhpClass classInterface = PhpElementsUtil.getClassInterface(project, fqn);
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
            for (String fqn : FileBasedIndex.getInstance().getValues(UxTemplateStubIndex.KEY, twigComponentName, GlobalSearchScope.allScope(project))) {
                components.put(twigComponentName, fqn);
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
}
