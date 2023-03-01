package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionKeyReference extends PsiPolyVariantReferenceBase<PsiElement> {
    @NotNull
    final private StringLiteralExpression element;

    @NotNull
    final private Set<String> formTypes = Stream
        .of("form", "Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType")
        .collect(Collectors.toCollection(HashSet::new));

    public FormExtensionKeyReference(@NotNull StringLiteralExpression element, @Nullable String formType) {
        super(element);
        this.element = element;

        if(formType != null) {
            this.formTypes.add(formType);
        }
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        return PsiElementResolveResult.createResults(
            FormOptionsUtil.getFormExtensionsKeysTargets(element, formTypes.toArray(new String[0]))
        );
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return FormOptionsUtil.getFormExtensionKeysLookupElements(getElement().getProject(), formTypes.toArray(new String[0])).toArray();
    }
}
