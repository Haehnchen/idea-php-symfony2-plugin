package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
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
        FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(getElement().getProject(), FormTypeServiceParser.class);

        FormTypeMap map = formTypeServiceParser.getFormTypeMap();
        for(String key : map.getMap().keySet()) {
            lookupElements.add(new FormTypeLookup(key, map.getMap().get(key)));
        }

        for(Map.Entry<String, FormTypeClass> entry: FormUtil.getFormTypeClasses(getElement().getProject()).entrySet()) {
            lookupElements.add(new FormTypeLookup(entry.getValue().getPhpClass().getName(), entry.getValue().getName()));
        }

        return lookupElements.toArray();
    }

}
