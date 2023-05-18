package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class ExpressionLanguageSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey NUMBER = createTextAttributesKey("NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = createTextAttributesKey("STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey IDENTIFIER = createTextAttributesKey("IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey KEYWORD = createTextAttributesKey("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new ExpressionLanguageLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(ExpressionLanguageTypes.NUMBER)) {
            return NUMBER_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.ID)) {
            return IDENTIFIER_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.TRUE)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.FALSE)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.NULL)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_IN)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_NOT_IN)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_MATCHES)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_AND_KW)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_OR_KW)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(ExpressionLanguageTypes.OP_NOT_KW)) {
            return KEYWORD_KEYS;
        }

        return EMPTY_KEYS;
    }
}
