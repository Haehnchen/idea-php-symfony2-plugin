package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.component;

import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.XmlHighlighterColors;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.highlighter.PhpHighlightingData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLHighlighter;
import org.jetbrains.yaml.YAMLLanguage;

import javax.swing.*;
import java.awt.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaretOverlayComponent extends JComponent {

    private final Editor editor;
    private final String content;
    private final int offset;
    private final Language language;
    private int horizontalMargin = 20;

    public CaretOverlayComponent(@NotNull Editor editor, @NotNull String content, int offset, @NotNull Language language) {
        this.editor = editor;
        this.content = content;
        this.offset = offset;
        this.language = language;
    }

    @Override
    protected void paintComponent(Graphics g) {

        if(language == XMLLanguage.INSTANCE) {
            g.setColor(
                editor.getColorsScheme().getAttributes(XmlHighlighterColors.XML_COMMENT).getForegroundColor()
            );
        } else if(language == YAMLLanguage.INSTANCE) {
            g.setColor(
                editor.getColorsScheme().getAttributes(YAMLHighlighter.COMMENT).getForegroundColor()
            );
        } else if(language == PhpLanguage.INSTANCE) {
            g.setColor(
                editor.getColorsScheme().getAttributes(PhpHighlightingData.COMMENT).getForegroundColor()
            );
        }

        g.setFont(editor.getColorsScheme().getFont(EditorFontType.CONSOLE_ITALIC));

        int verticalAlignment = editor.getLineHeight() - editor.getColorsScheme().getEditorFontSize();

        int offset = editor.getDocument().getLineEndOffset(StringUtil.offsetToLineNumber(editor.getDocument().getCharsSequence(), this.offset));
        Point point = editor.visualPositionToXY(editor.offsetToVisualPosition(offset));
        g.drawString(content, point.x + horizontalMargin, point.y + editor.getLineHeight() - verticalAlignment);
    }
}
