package fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi;

import com.intellij.psi.tree.IElementType;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ExpressionLanguageTokenType extends IElementType {

    public ExpressionLanguageTokenType(@NonNls @NotNull String debugName) {
        super(debugName, ExpressionLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return " ExpressionLanguageType." + super.toString();
    }
}
