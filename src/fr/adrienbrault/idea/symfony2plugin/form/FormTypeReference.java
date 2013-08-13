package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeMap;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        String formType = element.getContents();
        FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(getElement().getProject(), FormTypeServiceParser.class);

        String serviceName = formTypeServiceParser.getFormTypeMap().getServiceName(formType);
        if(serviceName == null) {
            return null;
        }

        String serviceClass = ServiceXmlParserFactory.getInstance(getElement().getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        if (null == serviceClass) {
            return null;
        }

        List<ResolveResult> resolveResults = PhpElementsUtil.getClassInterfaceResolveResult(this.getElement().getProject(), serviceClass);
        if(resolveResults.size() == 0) {
            return null;
        }

        return resolveResults.iterator().next().getElement();
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();
        FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(getElement().getProject(), FormTypeServiceParser.class);

        FormTypeMap map = formTypeServiceParser.getFormTypeMap();
        for(String key : map.getMap().keySet()) {
            lookupElements.add(new FormTypeLookup(key, map.getMap().get(key)));
        }

        return lookupElements.toArray();
    }

}
