package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormExtensionKeyReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;
    private String[] formTypes = new String[] { "form" };

    public FormExtensionKeyReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    public FormExtensionKeyReference(@NotNull StringLiteralExpression element, String... formTypes) {
        this(element);
        this.formTypes = formTypes;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Map<String, FormOption> test = FormOptionsUtil.getFormExtensionKeys(this.getElement().getProject(), this.formTypes);
        String value = this.element.getContents();

        if(!test.containsKey(value)) {
          return null;
        }

        String className = test.get(value).getFormClass().getPhpClass().getPresentableFQN();

        PsiElement[] psiElements = PhpElementsUtil.getPsiElementsBySignature(this.element.getProject(), "#M#C\\" + className + ".setDefaultOptions");
        if(psiElements.length == 0) {
            return null;
        }

        PsiElement keyValue = PhpElementsUtil.findArrayKeyValueInsideReference(psiElements[0], "setDefaults", value);
        if(keyValue != null) {
            return keyValue;
        }

        return psiElements[0];
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(FormOption formOption: FormOptionsUtil.getFormExtensionKeys(this.getElement().getProject(), this.formTypes).values()) {
            lookupElements.add(FormOptionsUtil.getOptionLookupElement(formOption));
        }

        return lookupElements.toArray();
    }

}
