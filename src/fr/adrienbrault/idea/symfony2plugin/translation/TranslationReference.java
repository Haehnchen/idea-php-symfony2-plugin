package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private String domainName = null;
    private ParameterBag parameterBag = null;

    public TranslationReference(@NotNull StringLiteralExpression element, String domain, ParameterBag currentParameter ) {
        super(element);

        domainName = domain;
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

        // read translations;
        // @TODO: move caching out of Symfony2ProjectComponent
        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        TranslationStringMap map = symfony2ProjectComponent.getTranslationMap();
        if(map == null) {
            return lookupElements.toArray();
        }

        // every translation method call looks like:
        // ('string_id', 'arguments', 'domain')
        // so try to filter as much as we can

        // we are on domain parameter
        if(parameterBag.getIndex() == 2) {
            for(String domainKey : map.getDomainList()) {
                lookupElements.add(new TranslatorLookupElement(domainKey, domainKey));
            }
            return lookupElements.toArray();
        }


        // we can filter on messages domain
        if(domainName == null) {
            domainName = "messages";
        }

        ArrayList<String> domainMap = map.getDomainMap(domainName);
        if(domainMap == null) {
            return lookupElements.toArray();
        }

        for(String stringId : domainMap) {
            lookupElements.add(new TranslatorLookupElement(stringId, domainName));
        }

        return lookupElements.toArray();
    }

}
