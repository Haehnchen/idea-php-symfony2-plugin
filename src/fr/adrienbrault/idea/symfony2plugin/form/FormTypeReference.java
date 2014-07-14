package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.dict.EnumFormTypeSource;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;

    public FormTypeReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return FormUtil.getFormTypeToClass(getElement().getProject(), element.getContents());
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        final List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        FormUtil.FormTypeCollector collector = new FormUtil.FormTypeCollector(getElement().getProject()).collect();

        for(Map.Entry<String, FormTypeClass> entry: collector.getFormTypesMap().entrySet()) {
            String name = entry.getValue().getName();
            String typeText = entry.getValue().getPhpClassName();

            PhpClass phpClass = entry.getValue().getPhpClass();
            if(phpClass != null) {
                typeText = phpClass.getName();
            }

            FormTypeLookup formTypeLookup = new FormTypeLookup(typeText, name);
            if(entry.getValue().getSource() == EnumFormTypeSource.INDEX) {
                formTypeLookup.withWeak(true);
            }

            lookupElements.add(formTypeLookup);
        }

        return lookupElements.toArray();
    }

}
