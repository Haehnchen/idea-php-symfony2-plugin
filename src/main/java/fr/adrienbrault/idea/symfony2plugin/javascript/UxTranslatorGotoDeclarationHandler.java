package fr.adrienbrault.idea.symfony2plugin.javascript;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UxTranslatorGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final String FUNCTION_NAME = "trans";
    private static final String DEFAULT_DOMAIN = "messages";

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
        if (psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement) || psiElement.getNode().getElementType() != JSTokenTypes.STRING_LITERAL || !(psiElement.getContext() instanceof JSLiteralExpression literal)) {
            return null;
        }

        if (!(literal.getParent() instanceof JSArgumentList arguments) || !isTransCall(arguments)) {
            return null;
        }

        JSExpression[] expressions = arguments.getArguments();
        if (expressions.length == 0 || expressions[0] != literal) {
            return null;
        }

        String key = literal.getStringValue();
        if (key == null || key.isBlank()) {
            return new PsiElement[0];
        }

        return TranslationUtil.getTranslationPsiElements(psiElement.getProject(), key, getDomain(expressions));
    }

    private static boolean isTransCall(@NotNull JSArgumentList arguments) {
        return arguments.getParent() instanceof JSCallExpression call
            && call.getMethodExpression() instanceof JSReferenceExpression reference
            && FUNCTION_NAME.equals(reference.getReferenceName());
    }

    @NotNull
    private static String getDomain(@NotNull JSExpression[] expressions) {
        if (expressions.length < 3 || !(expressions[2] instanceof JSLiteralExpression domain)) {
            return DEFAULT_DOMAIN;
        }

        String value = domain.getStringValue();

        return value == null || value.isBlank() ? DEFAULT_DOMAIN : value;
    }
}
