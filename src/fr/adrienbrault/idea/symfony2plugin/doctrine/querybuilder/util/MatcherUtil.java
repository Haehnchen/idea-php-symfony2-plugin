package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.Nullable;

public class MatcherUtil {

    private static MethodMatcher.CallToSignature[] SELECT_FIELDS = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "orderBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "addOrderBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "select"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "addSelect"),
    };

    @Nullable
    public static MethodMatcher.MethodMatchParameter matchPropertyField(PsiElement psiElement) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
            .withSignature(SELECT_FIELDS)
            .match();

        if(methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 0)
                .withSignature(SELECT_FIELDS)
                .match();
        }

        return methodMatchParameter;
    }

    @Nullable
    public static MethodMatcher.MethodMatchParameter matchJoin(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 0)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "join")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin")
            .match();
    }



}
