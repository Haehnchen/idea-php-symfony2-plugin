package fr.adrienbrault.idea.symfony2plugin.security;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageCallExpr;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageLiteralExpr;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageRefExpr;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageStringLiteral;
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationExpressionGotoCompletionRegistrar implements GotoCompletionRegistrar {

    private static final String SECURITY_ANNOTATION = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security";

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // "@Security("is_granted('POST_SHOW', post) and has_role('ROLE_ADMIN')")"
        registrar.register(
            PlatformPatterns.psiElement()
                .withParent(PlatformPatterns
                    .psiElement(ExpressionLanguageStringLiteral.class)
                    .withParent(PlatformPatterns
                        .psiElement(ExpressionLanguageLiteralExpr.class)
                        .withParent(PlatformPatterns
                            .psiElement(ExpressionLanguageCallExpr.class)
                            .withFirstChild(PlatformPatterns
                                .psiElement(ExpressionLanguageRefExpr.class)
                                .withText(StandardPatterns.string().oneOf("has_role", "is_granted"))
                            )
                        )
                    )
                ),
            MyGotoCompletionProvider::new
        );
    }

    /**
     * "@Security("has_role('ROLE_FOOBAR')")"
     * "@Security("is_granted('POST_SHOW', post) and has_role('ROLE_ADMIN')")"
     */
    private static class MyGotoCompletionProvider extends GotoCompletionProvider {
        MyGotoCompletionProvider(@NotNull PsiElement psiElement) {
            super(psiElement);
        }

        @Override
        public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
            final CompletionResultSet resultSet = arguments.getResultSet();
            String blockNamePrefix = resultSet.getPrefixMatcher().getPrefix();

            CompletionResultSet myResultSet = resultSet.withPrefixMatcher(blockNamePrefix);

            VoterUtil.LookupElementPairConsumer consumer = new VoterUtil.LookupElementPairConsumer();
            VoterUtil.visitAttribute(getProject(), consumer);
            myResultSet.addAllElements(consumer.getLookupElements());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            var text = getElement().getText();
            if (text.length() < 2) {
                return Collections.emptyList();
            }

            // Strip quotes
            var role = text.substring(1, text.length() - 1);

            var targets = new HashSet<PsiElement>();
            VoterUtil.visitAttribute(getProject(), pair -> {
                if(pair.getFirst().equals(role)) {
                    targets.add(pair.getSecond());
                }
            });

            return targets;
        }
    }
}
