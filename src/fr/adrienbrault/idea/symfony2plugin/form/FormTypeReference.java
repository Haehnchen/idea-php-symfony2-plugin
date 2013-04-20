package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        if(parameterBag == null) {
            return lookupElements.toArray();
        }

        if(parameterBag.getIndex() != 1) {
            return lookupElements.toArray();
        }

        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        FormTypeMap map = symfony2ProjectComponent.getFormTypeMap();

        if(map == null) {
            return lookupElements.toArray();
        }

        for(String key : map.getMap().keySet()) {
            lookupElements.add(new FormTypeLookup(key, map.getMap().get(key)));
        }

        return lookupElements.toArray();
    }

}
