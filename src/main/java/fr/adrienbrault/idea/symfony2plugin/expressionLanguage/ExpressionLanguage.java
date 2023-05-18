package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lang.Language;

public class ExpressionLanguage extends Language {

    public static final ExpressionLanguage INSTANCE = new ExpressionLanguage();

    private ExpressionLanguage() {
        super("Symfony Expression Language");
    }
}
