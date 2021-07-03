package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionLanguageBraceMatcher implements PairedBraceMatcher {
    private static BracePair[] PAIRS = {
        new BracePair(
            ExpressionLanguageTypes.L_ROUND_BRACKET,
            ExpressionLanguageTypes.R_ROUND_BRACKET,
            true
        ),
        new BracePair(
            ExpressionLanguageTypes.L_SQUARE_BRACKET,
            ExpressionLanguageTypes.R_SQUARE_BRACKET,
            true
        ),
        new BracePair(
            ExpressionLanguageTypes.L_CURLY_BRACKET,
            ExpressionLanguageTypes.R_CURLY_BRACKET,
            true
        ),
    };

    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return false;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
