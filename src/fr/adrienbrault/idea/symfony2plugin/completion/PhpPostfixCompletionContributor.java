package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.PostfixTemplateLookupElement;
import org.jetbrains.annotations.NotNull;

public class PhpPostfixCompletionContributor extends CompletionContributor {

    public PhpPostfixCompletionContributor() {
        extend(CompletionType.BASIC, getPostfixPattern(), new PostfixTemplatesCompletionProvider());
    }

    private PsiElementPattern.Capture<PsiElement> getPostfixPattern() {
        return PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement().withText("."));
    }

    public class PostfixTemplatesCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement psiElement = completionParameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            PsiElement prevPsi = psiElement.getPrevSibling();

            // on incomplete code
            if(prevPsi == null) {
                PsiElement parent = completionParameters.getPosition().getParent();
                if(parent != null) {
                    PsiElement prevSibling = parent.getPrevSibling();
                    if(prevSibling != null) {
                        prevPsi = prevSibling.getPrevSibling();
                    }
                }
            }

            if(prevPsi == null || !isValidForPostfix(prevPsi)) {
                return;
            }

            completionResultSet.addElement(new PostfixTemplateLookupElement("if", "if($EXPR$$END$) {\n\n}"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("ifn", "if(!$EXPR$$END$) {\n\n}"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("null", "if($EXPR$ === null$END$) {\n\n}"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("notnull", "if($EXPR$ !== null$END$) {\n\n}"));

            completionResultSet.addElement(new PostfixTemplateLookupElement("isset", "if(isset($EXPR$$END$)) {\n\n}"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("empty", "if(empty($EXPR$$END$)) {\n\n}"));

            completionResultSet.addElement(new PostfixTemplateLookupElement("notisset", "if(!isset($EXPR$)$END$) {\n\n}"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("notempty", "if(!empty($EXPR$)$END$) {\n\n}"));

            completionResultSet.addElement(new PostfixTemplateLookupElement("return", "return $EXPR$$END$"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("pr", "print_r($EXPR$)$END$"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("d", "var_dump($EXPR$)$END$"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("e", "echo $EXPR$$END$"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("p", "print $EXPR$$END$"));

            completionResultSet.addElement(new PostfixTemplateLookupElement("foreach", "foreach ($EXPR$ as $END$) {\n" +
                "\t\n" +
                "}")
            );

            completionResultSet.addElement(new PostfixTemplateLookupElement("for", "$x = count($EXPR$);\n" +
                    "for ($i = 1; $i <= $x$END$; $i++) {\n" +
                    "    \n" +
                    "}")
            );

            completionResultSet.addElement(new PostfixTemplateLookupElement("instanceof", "$EXPR$ instanceof $END$"));
            completionResultSet.addElement(new PostfixTemplateLookupElement("inst", "$EXPR$ instanceof $END$"));

            completionResultSet.addElement(new PostfixTemplateLookupElement("try", "try {\n" +
                "\t$EXPR$$END$;\n" +
                "} catch (\\Exception $e) {\n" +
                "\t\n" +
                "}")
            );

        }

        private boolean isValidForPostfix(PsiElement prevPsi) {

            // on incomplete code
            if(prevPsi instanceof Variable || prevPsi instanceof MethodReference || prevPsi instanceof ArrayAccessExpression) {
                return true;
            }

            IElementType elementType = prevPsi.getNode().getElementType();
            if(elementType == PhpTokenTypes.VARIABLE) {
                return true;
            }

            if(elementType == PhpElementTypes.CONCATENATION_EXPRESSION) {
                PsiElement child = prevPsi.getFirstChild();
                return child instanceof Variable || child instanceof MethodReference || child instanceof ArrayAccessExpression;
            }

            return false;
        }

    }

}
