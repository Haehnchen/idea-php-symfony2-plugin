package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeLookup;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
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

        MethodReference[] formBuilderTypes = FormUtil.getFormBuilderTypes(method);
        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(MethodReference methodReference: formBuilderTypes) {
            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if(fieldName != null) {
                lookupElements.add(LookupElementBuilder.create(fieldName));
            }
        }

        return lookupElements.toArray();
    }

}
