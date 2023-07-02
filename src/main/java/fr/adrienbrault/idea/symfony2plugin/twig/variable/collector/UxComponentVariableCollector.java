package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Provide "symfony ux" component variables support
 *
 * @link https://symfony.com/bundles/ux-twig-component/current/index.html#exposeintemplate-attribute
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxComponentVariableCollector implements TwigFileVariableCollector {
    private static final String ATTRIBUTE_EXPOSE_IN_TEMPLATE = "\\Symfony\\UX\\TwigComponent\\Attribute\\ExposeInTemplate";

    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getElement().getContainingFile();
        if (!(psiFile instanceof TwigFile)) {
            return;
        }

        for (PhpClass phpClass : UxUtil.getComponentClassesForTemplateFile(parameter.getProject(), psiFile)) {
            for (Field field : phpClass.getFields()) {
                if (field.getModifier().isPublic()) {
                    for (String name : getExposeName(field)) {
                        variables.put(name, new PsiVariable(field.getType().getTypes(), field));
                    }
                }

                if (field.getModifier().isPrivate() && field.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).size() > 0) {
                    for (String name : getExposeName(field)) {
                        variables.put(name, new PsiVariable(field.getType().getTypes(), field));
                    }
                }
            }

            for (Method method : phpClass.getMethods()) {
                if (method.getAccess().isPublic() && method.getAttributes(ATTRIBUTE_EXPOSE_IN_TEMPLATE).size() > 0) {
                    for (String name : getExposeName(method)) {
                        variables.put(name, new PsiVariable(method.getType().getTypes(), method));
                    }
                }
            }
        }
    }

    private Collection<String> getExposeName(@NotNull PhpAttributesOwner phpAttributesOwner) {
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
                names.add(name);
            }
        }

        return names;
    }
}
