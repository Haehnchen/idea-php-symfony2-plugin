package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private ParameterBag parameterBag = null;

    public FormTypeReference(@NotNull StringLiteralExpression element, ParameterBag currentParameter ) {
        super(element);
        parameterBag = currentParameter;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();
        if(parameterBag == null || parameterBag.getIndex() != 1) {
            return lookupElements.toArray();
        }

        ServiceXmlParserFactory xmlParser = ServiceXmlParserFactory.getInstance(getElement().getProject(), FormTypeServiceParser.class);
        Object formTypeMap = xmlParser.parser();

        if(!(formTypeMap instanceof FormTypeMap)) {
            return lookupElements.toArray();
        }

        FormTypeMap map = (FormTypeMap) formTypeMap;
        for(String key : map.getMap().keySet()) {
            lookupElements.add(new FormTypeLookup(key, map.getMap().get(key)));
        }

        return lookupElements.toArray();
    }

}
