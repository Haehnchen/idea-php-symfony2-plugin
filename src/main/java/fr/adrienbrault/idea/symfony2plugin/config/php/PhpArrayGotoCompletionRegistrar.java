package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.completion.DecoratedServiceCompletionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers service-id completion for PHP array `decorates` and `parent` values, eg:
 * `'decorates' => 'mailer'`
 * `'parent' => 'app.abstract_mailer'`
 */
public class PhpArrayGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            psiElement -> {
                if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                    return null;
                }

                PhpArrayServiceUtil.ServiceConfigPath keyPath = PhpArrayServiceUtil.getKeyPath(stringLiteralExpression);
                if (keyPath == null || !keyPath.isDecoratesOrParent()) {
                    return null;
                }

                return new DecoratesParentContributor(stringLiteralExpression);
            }
        );
    }

    private static class DecoratesParentContributor extends DecoratedServiceCompletionProvider {
        private DecoratesParentContributor(@NotNull StringLiteralExpression element) {
            super(element);
        }

        @Nullable
        @Override
        public String findClassForElement(@NotNull PsiElement psiElement) {
            return PhpArrayServiceUtil.getCurrentServiceClass(psiElement);
        }

        @Nullable
        @Override
        public String findIdForElement(@NotNull PsiElement psiElement) {
            return PhpArrayServiceUtil.getServiceId(psiElement);
        }
    }
}
