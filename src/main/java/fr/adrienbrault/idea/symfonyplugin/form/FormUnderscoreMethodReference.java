package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUnderscoreMethodReference extends PsiPolyVariantReferenceBase<StringLiteralExpression> {
    @NotNull
    private PhpClass phpClass;

    public FormUnderscoreMethodReference(@NotNull StringLiteralExpression element, @NotNull PhpClass phpClass) {
        super(element);
        this.phpClass = phpClass;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String contents = getElement().getContents();
        if(org.apache.commons.lang.StringUtils.isBlank(contents)) {
            return new ResolveResult[0];
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        Set<String> methods = getCamelizeAndUnderscoreString(contents);

        // provide setter fallback for non model class or or unknown methods
        for (String value : methods) {
            Method method = phpClass.findMethodByName("set" + value);
            if (method != null) {
                psiElements.add(method);
            }
        }

        // property path
        psiElements.addAll(this.phpClass.getFields().stream()
            .filter(field -> !field.isConstant() && field.getModifier().isPublic() && methods.contains(field.getName()))
            .collect(Collectors.toList())
        );

        return PsiElementResolveResult.createResults(psiElements);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        // provide setter fallback for non model class or unknown methods
        for(Method method: this.phpClass.getMethods()) {
            String name = method.getName();
            if(name.length() > 3 && name.startsWith("set")) {
                lookupElements.add(new PhpFormPropertyMethodLookupElement(method, StringUtils.lcfirst(name.substring(3))));
            }
        }

        // Symfony\Component\PropertyAccess\PropertyAccessor::getWriteAccessInfo
        // property: public $foobar
        lookupElements.addAll(this.phpClass.getFields().stream()
            .filter(field -> !field.isConstant() && field.getModifier().isPublic())
            .map(field -> new PhpFormPropertyMethodLookupElement(field, field.getName()))
            .collect(Collectors.toList())
        );

        return lookupElements.toArray();
    }

    @NotNull
    private Set<String> getCamelizeAndUnderscoreString(@NotNull String string) {
        TreeSet<String> strings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        string = StringUtils.lcfirst(string);

        strings.addAll(Arrays.asList(
            StringUtils.underscore(string),
            StringUtils.camelize(string),
            string
        ));

        return strings;
    }
}
