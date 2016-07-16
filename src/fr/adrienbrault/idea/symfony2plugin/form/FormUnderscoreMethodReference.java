package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelFieldLookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUnderscoreMethodReference  extends PsiPolyVariantReferenceBase<PsiElement> {

    private StringLiteralExpression element;
    private PhpClass phpClass;

    public FormUnderscoreMethodReference(@NotNull StringLiteralExpression element, PhpClass phpClass) {
        super(element);
        this.element = element;
        this.phpClass = phpClass;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        Collection<PsiElement> psiElements = new ArrayList<>();

        String value = element.getContents();
        for(DoctrineModelField field: EntityHelper.getModelFields(this.phpClass)) {
            if(value.equals(field.getName())) {
                psiElements.addAll(field.getTargets());
            }
        }

        // provide setter fallback for non model class or or unknown methods
        String methodCamel = StringUtils.camelize(element.getContents());
        Method method = phpClass.findMethodByName("set" + methodCamel);
        if (method != null) {
            psiElements.add(method);
        }

        return PsiElementResolveResult.createResults(psiElements);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<>();

        Set<String> strings = new HashSet<>();
        for(DoctrineModelField field: EntityHelper.getModelFields(this.phpClass)) {
            addCamelUnderscoreName(strings, org.apache.commons.lang.StringUtils.trim(field.getName()));
            lookupElements.add(new DoctrineModelFieldLookupElement(field).withBoldness(true));
        }

        // provide setter fallback for non model class or unknown methods
        for(Method method: this.phpClass.getMethods()) {
            String name = method.getName();
            if(name.length() > 3 && name.startsWith("set")) {
                name = name.substring(3);
                if(!isCamelUnderscoreEqual(strings, name)) {
                    // @TODO: should we really stay this underscore way?
                    lookupElements.add(new PhpUnderscoreMethodLookupElement(method));
                }
            }
        }

        return lookupElements.toArray();
    }

    private boolean isCamelUnderscoreEqual(Set<String> strings, String string) {
        return strings.contains(string) || strings.contains(StringUtils.camelize(string)) || strings.contains(StringUtils.underscore(string));
    }

    private void addCamelUnderscoreName(Set<String> strings, String string) {
        strings.add(StringUtils.camelize(string));
        strings.add(StringUtils.underscore(string));
    }

}
