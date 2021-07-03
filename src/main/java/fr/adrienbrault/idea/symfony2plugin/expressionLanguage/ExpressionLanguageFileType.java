package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.openapi.fileTypes.LanguageFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ExpressionLanguageFileType extends LanguageFileType {

    public static final ExpressionLanguageFileType INSTANCE = new ExpressionLanguageFileType();

    private ExpressionLanguageFileType() {
        super(ExpressionLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Expression Language File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Expression Language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "sfel";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return Symfony2Icons.SYMFONY;
    }
}
