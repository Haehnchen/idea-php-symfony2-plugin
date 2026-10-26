package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Resolves nested form views and the children of CollectionType and RepeatedType.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NestedFormFieldResolver implements TwigTypeResolver {
    private static final String COLLECTION_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CollectionType";
    private static final String REPEATED_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\RepeatedType";
    private static final String TEXT_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType";
    private static final Set<String> CHILD_TYPE_OPTIONS = Set.of("entry_type", "type", "first_name", "second_name", "allow_add", "prototype");
    private static final Set<String> CHILD_OPTION_ARRAYS = Set.of("entry_options", "prototype_options", "options", "first_options", "second_options");

    public void resolve(@NotNull Project project, Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (previousElements == null) {
            return;
        }

        if (previousElements.isEmpty()) {
            for (TwigTypeContainer root : List.copyOf(targets)) {
                if (root.getFormFieldDataHolder() != null && root.getStringElement() == null) {
                    visitChildFields(project, root.getFormFieldDataHolder(), targets);
                }
            }
            return;
        }

        if (previousElement == null) {
            return;
        }
        if ("vars".equals(typeName)) {
            resolveFieldVars(project, previousElement, targets);
            return;
        }

        for (TwigTypeContainer previous : previousElement) {
            FormFieldDataHolder field = previous.getFormFieldDataHolder();
            if (field != null && typeName.equals(previous.getStringElement())) {
                targets.add(TwigTypeContainer.forFormView(field));
                visitChildFields(project, field, targets);
            } else if ("children".equals(typeName) && previous.getStringElement() == null) {
                if (field != null) {
                    targets.add(TwigTypeContainer.forFormChildren(field));
                    visitChildFields(project, field, targets);
                } else if (previous.getFormViewDataHolder() != null) {
                    FormFieldResolver.visitFormFields(project, previous.getFormViewDataHolder().formTypeFqns(), child -> targets.add(toTwigTypeContainer(child)));
                }
            }
        }
    }

    private static void resolveFieldVars(@NotNull Project project, @NotNull Collection<TwigTypeContainer> previous, @NotNull Collection<TwigTypeContainer> targets) {
        for (TwigTypeContainer view : previous) {
            FormFieldDataHolder field = view.getFormFieldDataHolder();
            if (field == null || view.getStringElement() != null || !view.getTypes().contains("\\Symfony\\Component\\Form\\FormView")) {
                continue;
            }
            FormVarsResolver.attachVars(project, targets);
            FormFieldDataHolder prototype = getCollectionPrototype(field);
            if (prototype != null) {
                targets.add(new TwigTypeContainer("prototype", prototype));
            }
        }
    }

    private static void visitChildFields(@NotNull Project project, @NotNull FormFieldDataHolder field, @NotNull Collection<TwigTypeContainer> targets) {
        if (REPEATED_TYPE.equals(field.fieldTypeFqn())) {
            FormFieldDataHolder first = childField(field, "type", childOptions(field.options(), "options", "first_options"));
            FormFieldDataHolder second = childField(field, "type", childOptions(field.options(), "options", "second_options"));
            targets.add(new TwigTypeContainer(field.options().getOrDefault("first_name", "first"), first));
            targets.add(new TwigTypeContainer(field.options().getOrDefault("second_name", "second"), second));
        } else if (field.fieldTypeFqn() != null && !COLLECTION_TYPE.equals(field.fieldTypeFqn())) {
            FormFieldResolver.visitFormFields(project, Collections.singleton(field.fieldTypeFqn()), child -> targets.add(toTwigTypeContainer(child)));
        }
    }

    @Nullable
    public static FormFieldDataHolder getCollectionEntry(@NotNull FormFieldDataHolder field) {
        return COLLECTION_TYPE.equals(field.fieldTypeFqn())
            ? childField(field, "entry_type", childOptions(field.options(), "entry_options"))
            : null;
    }

    @Nullable
    private static FormFieldDataHolder getCollectionPrototype(@NotNull FormFieldDataHolder field) {
        if (!COLLECTION_TYPE.equals(field.fieldTypeFqn()) || !"true".equals(field.options().get("allow_add")) || "false".equals(field.options().get("prototype"))) {
            return null;
        }
        return childField(field, "entry_type", childOptions(field.options(), "entry_options", "prototype_options"));
    }

    @NotNull
    private static FormFieldDataHolder childField(@NotNull FormFieldDataHolder parent, @NotNull String typeOption, @NotNull Map<String, String> options) {
        String type = parent.options().getOrDefault(typeOption, TEXT_TYPE);
        return new FormFieldDataHolder("\\" + StringUtils.stripStart(type, "\\"), parent.ownerFormTypeFqn(), parent.propertyName(), options);
    }

    @NotNull
    private static Map<String, String> childOptions(@NotNull Map<String, String> options, String... prefixes) {
        Map<String, String> child = new HashMap<>();
        for (String prefix : prefixes) {
            for (Map.Entry<String, String> option : options.entrySet()) {
                if (option.getKey().startsWith(prefix + ".")) {
                    child.put(option.getKey().substring(prefix.length() + 1), option.getValue());
                }
            }
        }
        return child;
    }

    private static TwigTypeContainer toTwigTypeContainer(@NotNull TwigFormField field) {
        return new TwigTypeContainer(field.name(), new FormFieldDataHolder(field.fieldTypeFqn(), field.ownerFormTypeFqn(), field.propertyName(), field.options()));
    }

    @NotNull
    static Map<String, String> getFieldOptions(@Nullable PsiElement options) {
        Map<String, String> values = new HashMap<>();
        collectFieldOptions(options, "", values, 0);
        return values;
    }

    // Keep nested entry/prototype options as primitive values, without retaining their PSI.
    private static void collectFieldOptions(@Nullable PsiElement options, @NotNull String prefix, @NotNull Map<String, String> values, int depth) {
        if (!(options instanceof ArrayCreationExpression array) || depth > 5) {
            return;
        }
        for (ArrayHashElement option : array.getHashElements()) {
            String key = PhpElementsUtil.getStringValue(option.getKey());
            if (key == null) {
                continue;
            }
            if (CHILD_OPTION_ARRAYS.contains(key)) {
                collectFieldOptions(option.getValue(), prefix + key + ".", values, depth + 1);
            } else if (CHILD_TYPE_OPTIONS.contains(key)) {
                String value = PhpElementsUtil.getStringValue(option.getValue());
                if (value == null && option.getValue() != null) {
                    String text = option.getValue().getText();
                    if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                        value = text.toLowerCase(Locale.ROOT);
                    }
                }
                if (value != null) {
                    values.put(prefix + key, value);
                }
            }
        }
    }
}
