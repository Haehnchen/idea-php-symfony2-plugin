package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.Nullable;

public class MatcherUtil {

    @Nullable
    public static MethodMatcher.MethodMatchParameter matchPropertyField(PsiElement psiElement) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "orderBy")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "addSelect")
            .match();

        if(methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 0)
                .withSignature("\\Doctrine\\ORM\\QueryBuilder", "orderBy")
                .withSignature("\\Doctrine\\ORM\\QueryBuilder", "addSelect")
                .match();
        }

        return methodMatchParameter;
    }
}
