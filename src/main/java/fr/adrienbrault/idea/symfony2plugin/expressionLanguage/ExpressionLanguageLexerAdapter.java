package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lexer.FlexAdapter;

public class ExpressionLanguageLexerAdapter extends FlexAdapter {
    public ExpressionLanguageLexerAdapter() {
        super(new ExpressionLanguageLexer());
    }
}
