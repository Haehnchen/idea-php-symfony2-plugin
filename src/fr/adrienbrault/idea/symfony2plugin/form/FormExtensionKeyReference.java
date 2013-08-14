package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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
        HashMap<String, String> test = FormOptionsUtil.getFormExtensionKeys(this.getElement().getProject(), this.formTypes);
        String value = this.element.getContents();

        if(!test.containsKey(value)) {
          return null;
        }

        String className = test.get(value);

        PsiElement[] psiElements = PhpElementsUtil.getPsiElementsBySignature(this.element.getProject(), "#M#C\\" + className + ".setDefaultOptions");
        if(psiElements.length == 0) {
            return null;
        }

        return psiElements[0];
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(Map.Entry<String, String> extension: FormOptionsUtil.getFormExtensionKeys(this.getElement().getProject(), this.formTypes).entrySet()) {
            String typeText = extension.getValue();
            if(typeText.lastIndexOf("\\") != -1) {
                typeText = typeText.substring(typeText.lastIndexOf("\\") + 1);
            }

            if(typeText.endsWith("Extension")) {
                typeText = typeText.substring(0, typeText.length() - 9);
            }

            lookupElements.add(LookupElementBuilder.create(extension.getKey())
                .withTypeText(typeText)
            );
        }

        return lookupElements.toArray();
    }

}
