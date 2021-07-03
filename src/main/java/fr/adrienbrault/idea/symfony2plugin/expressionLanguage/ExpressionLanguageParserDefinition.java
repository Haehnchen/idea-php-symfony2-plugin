package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageFile;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageTypes;
import org.jetbrains.annotations.NotNull;

public class ExpressionLanguageParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE = new IFileElementType(ExpressionLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new ExpressionLanguageLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new ExpressionLanguageParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.create(ExpressionLanguageTypes.STRING);
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return ExpressionLanguageTypes.Factory.createElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new ExpressionLanguageFile(viewProvider);
    }
}
