package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigFormField;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldNameReference extends PsiReferenceBase<StringLiteralExpression> {

    private final PhpClass formType;

    public FormFieldNameReference(@NotNull StringLiteralExpression element, @NotNull PhpClass formType) {
        super(element, element.getValueRange());
        this.formType = formType;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        for (TwigFormField field : getFields()) {
            if (!field.name().equals(myElement.getContents())) {
                continue;
            }

            PhpClass owner = PhpElementsUtil.getClassInterface(myElement.getProject(), field.ownerFormTypeFqn());
            if (owner == null) {
                continue;
            }
            if (field.propertyName() != null) {
                return FormFieldResolver.resolveAttributeField(owner, field.propertyName());
            }

            return resolveBuilderField(owner, field.name());
        }

        return null;
    }

    @Nullable
    private static MethodReference resolveBuilderField(@NotNull PhpClass owner, @NotNull String name) {
        Method method = owner.findMethodByName("buildForm");
        if (method == null) {
            return null;
        }
        for (MethodReference call : FormUtil.getFormBuilderTypes(method)) {
            if (name.equals(PsiElementUtils.getMethodParameterAt(call, 0))) {
                return call;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Map<String, LookupElement> lookups = new LinkedHashMap<>();
        for (TwigFormField field : getFields()) {
            LookupElementBuilder lookup = LookupElementBuilder.create(field.name()).withIcon(Symfony2Icons.FORM_TYPE);
            if (field.fieldTypeFqn() != null) {
                lookup = lookup.withTypeText(field.fieldTypeFqn(), true);
            }
            lookups.put(field.name(), lookup);
        }
        return lookups.values().toArray(LookupElement[]::new);
    }

    @NotNull
    private List<TwigFormField> getFields() {
        List<TwigFormField> fields = new ArrayList<>();
        FormFieldResolver.visitFormFields(myElement.getProject(), Collections.singleton(formType.getFQN()), fields::add);
        return fields;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) {
        return myElement;
    }
}
