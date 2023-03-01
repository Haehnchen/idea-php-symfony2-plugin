package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldNameReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;
    private Method method;

    public FormFieldNameReference(@NotNull StringLiteralExpression element, Method method) {
        super(element);
        this.element = element;
        this.method = method;
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        MethodReference[] formBuilderTypes = FormUtil.getFormBuilderTypes(method);
        for(MethodReference methodReference: formBuilderTypes) {
            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if(fieldName != null && fieldName.equals(this.element.getContents())) {
                return methodReference;
            }
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return getFormLookups(method);
    }

    public static LookupElement[] getFormLookups(Method method) {

        MethodReference[] formBuilderTypes = FormUtil.getFormBuilderTypes(method);
        List<LookupElement> lookupElements = new ArrayList<>();

        for(MethodReference methodReference: formBuilderTypes) {
            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if(fieldName != null) {

                LookupElementBuilder lookup = LookupElementBuilder.create(fieldName).withIcon(Symfony2Icons.FORM_TYPE);
                String fieldType = PsiElementUtils.getMethodParameterAt(methodReference, 1);
                if(fieldType != null) {
                    lookup = lookup.withTypeText(fieldType, true);
                }

                lookupElements.add(lookup);
            }
        }

        return lookupElements.toArray(new LookupElement[0]);
    }

}
