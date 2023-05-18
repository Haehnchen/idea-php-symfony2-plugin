package fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguage;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguageFileType;
import org.jetbrains.annotations.NotNull;

public class ExpressionLanguageFile extends PsiFileBase {

    public ExpressionLanguageFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ExpressionLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return ExpressionLanguageFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Expression Language File";
    }
}
