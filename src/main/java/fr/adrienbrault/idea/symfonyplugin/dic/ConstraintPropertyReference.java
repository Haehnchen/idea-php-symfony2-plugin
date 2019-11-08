package fr.adrienbrault.idea.symfonyplugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConstraintPropertyReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final PhpClass constraintPhpClass;

    public ConstraintPropertyReference(StringLiteralExpression psiElement, PhpClass constraintPhpClass) {
        super(psiElement);
        this.constraintPhpClass = constraintPhpClass;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {

        List<PsiElement> psiElements = new ArrayList<>();

        String content = ((StringLiteralExpression) getElement()).getContents();
        for(Field field: constraintPhpClass.getFields()) {
            String name = field.getName();
            if(!field.isConstant() && field.getModifier().isPublic() && content.equals(name)) {
                psiElements.add(field);
            }
        }

        return PsiElementResolveResult.createResults(psiElements);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<>();

        for(Field field: constraintPhpClass.getFields()) {
            if(!field.isConstant() && field.getModifier().isPublic()) {

                LookupElementBuilder lookupElement = LookupElementBuilder.create(field.getName()).withIcon(Symfony2Icons.SYMFONY);

                String defaultValue = PhpElementsUtil.getStringValue(field.getDefaultValue());
                if(defaultValue != null) {
                    lookupElement = lookupElement.withTypeText(defaultValue, true);
                }

                lookupElements.add(lookupElement);
            }
        }

        return lookupElements.toArray();
    }

}
