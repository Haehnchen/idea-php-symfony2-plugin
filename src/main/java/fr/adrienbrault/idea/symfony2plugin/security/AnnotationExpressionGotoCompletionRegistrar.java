package fr.adrienbrault.idea.symfony2plugin.security;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
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
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationExpressionGotoCompletionRegistrar implements GotoCompletionRegistrar {

    private static final String SECURITY_ANNOTATION = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security";

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // "@Security("is_granted('POST_SHOW', post) and has_role('ROLE_ADMIN')")"
        registrar.register(
            PlatformPatterns.or(getDocTagStringPattern(), getAttributeStringPattern()),
            MyGotoCompletionProvider::new
        );
    }

    @NotNull
    private PsiElementPattern.Capture<PsiElement> getAttributeStringPattern() {
        // #[Security("is_granted('POST_SHOW')")]
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
                PhpTokenTypes.STRING_LITERAL
            ))
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(ParameterList.class)
                    .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                        .with(PhpDocInstancePatternCondition.INSTANCE)
                    )
                )
            );
    }

    @NotNull
    private PsiElementPattern.Capture<PsiElement> getDocTagStringPattern() {
        return PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_STRING)
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList)
                    .withParent(PlatformPatterns.psiElement(PhpDocTag.class)
                        .with(PhpDocInstancePatternCondition.INSTANCE)
                    )
                )
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

            // find caret position:
            // - "has_role('"
            // - "has_role('YAML_ROLE_"
            if(!blockNamePrefix.matches("^.*(has_role|is_granted)\\s*\\(\\s*['|\"][\\w-]*$")) {
                return;
            }

            // clear prefix caret string; for a clean completion independent from inside content
            String substring = blockNamePrefix.replaceAll("^(.*(has_role|is_granted)\\s*\\(\\s*['|\"])", "");
            CompletionResultSet myResultSet = resultSet.withPrefixMatcher(substring);

            myResultSet.addAllElements(VoterUtil.getVoterAttributeLookupElements(getProject()));
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = null;
            if(getElement().getNode().getElementType() == PhpDocTokenTypes.DOC_STRING) {
                // @Security
                PsiElement parent = getElement().getParent();
                if(!(parent instanceof StringLiteralExpression)) {
                    return Collections.emptyList();
                }

                contents = ((StringLiteralExpression) parent).getContents();
           } else {
                // @Security
                PsiElement parent = getElement().getParent();
                if (parent instanceof StringLiteralExpression) {
                    contents = ((StringLiteralExpression) parent).getContents();
                }
            }

            if (StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Collection<String> roles = new HashSet<>();
            for (String regex : new String[]{"is_granted\\s*\\(\\s*['|\"]([^'\"]+)['|\"]\\s*[\\)|,]", "has_role\\s*\\(\\s*['|\"]([^'\"]+)['|\"]\\s*\\)"}) {
                Matcher matcher = Pattern.compile(regex).matcher(contents);
                while(matcher.find()){
                    roles.add(matcher.group(1));
                }
            }

            if(roles.isEmpty()) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new HashSet<>();

            VoterUtil.visitAttribute(getProject(), pair -> {
                if(roles.contains(pair.getFirst())) {
                    targets.add(pair.getSecond());
                }
            });

            return targets;
        }
    }

    /**
     * Check if given PhpDocTag is instance of given Annotation class
     */
    private static class PhpDocInstancePatternCondition extends PatternCondition<PsiElement> {
        private static final PhpDocInstancePatternCondition INSTANCE = new PhpDocInstancePatternCondition();

        PhpDocInstancePatternCondition() {
            super("PhpDoc/Attribute Instance");
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            if (psiElement instanceof PhpDocTag) {
                return PhpElementsUtil.isEqualClassName(AnnotationUtil.getAnnotationReference((PhpDocTag) psiElement), SECURITY_ANNOTATION);
            } else if (psiElement instanceof PhpAttribute) {
                return SECURITY_ANNOTATION.equals(((PhpAttribute) psiElement).getFQN());
            }

            return false;
        }
    }
}
