package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.psi.TokenType;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageTypes;

public class ExpressionLanguageQuoteHandler extends SimpleTokenSetQuoteHandler {

    public ExpressionLanguageQuoteHandler() {
        super(ExpressionLanguageTypes.STRING, TokenType.BAD_CHARACTER);
    }

}
