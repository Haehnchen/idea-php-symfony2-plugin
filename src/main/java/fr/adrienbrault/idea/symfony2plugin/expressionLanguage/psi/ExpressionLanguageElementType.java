package fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi;

import com.intellij.psi.tree.IElementType;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ExpressionLanguageElementType extends IElementType {
    public ExpressionLanguageElementType(@NonNls @NotNull String debugName) {
        super(debugName, ExpressionLanguage.INSTANCE);
    }
}
