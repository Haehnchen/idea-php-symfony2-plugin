package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Collection;

/**
 * key: !php/enum Foo\Bar::BAZ
 * key: !php/enum Foo\Bar::BAZ->value
 */
public class EnumCaseYamlReference extends PsiPolyVariantReferenceBase<YAMLScalar> {

    private static final String SCOPE_OPERATOR = "::";
    private static final String VALUE_ACCESSOR = "->";

    public EnumCaseYamlReference(@NotNull YAMLScalar element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        var value = getElement().getTextValue();
        var scopeOperatorPos = value.indexOf(SCOPE_OPERATOR);
        if (scopeOperatorPos < 1) {
            return ResolveResult.EMPTY_ARRAY;
        }

        var className = value.substring(0, scopeOperatorPos);
        var caseName = getCaseName(value);

        Collection<PsiElement> targets = new ArrayList<>();
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(getElement().getProject(), className)) {
            if (phpClass.isEnum()) {
                phpClass.getEnumCases().stream()
                    .filter(enumCase -> caseName.equals(enumCase.getName()))
                    .forEach(targets::add);
            }
        }

        return PsiElementResolveResult.createResults(targets);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        var value = getValue();
        var scopeOperatorPos = value.indexOf(SCOPE_OPERATOR);
        if (scopeOperatorPos > 0) {
            var valueAccessorPos = value.indexOf(VALUE_ACCESSOR, scopeOperatorPos);
            newElementName = value.substring(0, scopeOperatorPos + SCOPE_OPERATOR.length())
                + newElementName
                + (valueAccessorPos > -1 ? value.substring(valueAccessorPos) : "");
        }

        return super.handleElementRename(newElementName);
    }

    @NotNull
    private static String getCaseName(@NotNull String value) {
        var caseName = value.substring(value.indexOf(SCOPE_OPERATOR) + SCOPE_OPERATOR.length());
        var valueAccessorPos = caseName.indexOf(VALUE_ACCESSOR);

        return valueAccessorPos > -1 ? caseName.substring(0, valueAccessorPos) : caseName;
    }
}
