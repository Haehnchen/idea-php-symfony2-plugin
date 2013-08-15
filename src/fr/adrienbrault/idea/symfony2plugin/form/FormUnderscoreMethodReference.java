package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUnderscoreMethodReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;
    private PhpClass phpClass;

    public FormUnderscoreMethodReference(@NotNull StringLiteralExpression element, PhpClass phpClass) {
        super(element);
        this.element = element;
        this.phpClass = phpClass;
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        String methodCamel = this.toCamelCase(element.getContents());
        PsiElement psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(this.getElement().getProject(), "#M#C\\" + phpClass.getPresentableFQN() + ".set" + methodCamel);
        if(psiElement != null) {
          return psiElement;
        }

        psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(this.getElement().getProject(), "#M#C\\" + phpClass.getPresentableFQN() + ".get" + methodCamel);
        if(psiElement != null) {
            return psiElement;
        }

        psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(this.getElement().getProject(), "#M#C\\" + phpClass.getPresentableFQN() + "." + methodCamel);
        if(psiElement != null) {
            return psiElement;
        }

        psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(this.getElement().getProject(), "#M#C\\" + phpClass.getPresentableFQN() + "." + element.getContents());
        if(psiElement != null) {
            return psiElement;
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(Method method: this.phpClass.getMethods()) {
            // @TODO: find symfony method filter
            if(method.getName().startsWith("set")) {
                lookupElements.add(new PhpUnderscoreMethodLookupElement(method));
            }
        }

        return lookupElements.toArray();
    }

    private String toCamelCase(String s) {
        StringBuilder sb = new StringBuilder();
        String[] x = s.replaceAll("[^A-Za-z]", " ").replaceAll("\\s+", " ")
            .trim().split(" ");

        for (int i = 0; i < x.length; i++) {
            if (i == 0) {
                x[i] = x[i].toLowerCase();
            } else {
                String r = x[i].substring(1);
                x[i] = String.valueOf(x[i].charAt(0)).toUpperCase() + r;

            }
            sb.append(x[i]);
        }
        return sb.toString();
    }


}
