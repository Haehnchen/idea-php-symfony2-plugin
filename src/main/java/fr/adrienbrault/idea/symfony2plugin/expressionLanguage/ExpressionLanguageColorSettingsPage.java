package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class ExpressionLanguageColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] ATTRIBUTE_DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Number", ExpressionLanguageSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", ExpressionLanguageSyntaxHighlighter.STRING),
            new AttributesDescriptor("Identifier", ExpressionLanguageSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Keyword", ExpressionLanguageSyntaxHighlighter.KEYWORD),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return Symfony2Icons.SYMFONY;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new ExpressionLanguageSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "article.getCommentCount(true) > 100 and article.category not in [\"misc\", null, true] === false";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Symfony Expression Language";
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return ATTRIBUTE_DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @Nullable
    Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}

