package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lookup element for Twig component blocks that inserts {@code <twig:block name="..."></twig:block>}
 * with the caret positioned between the tags.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigComponentBlockLookupElement extends LookupElement {
    private final String blockName;
    private final String typeText;

    public TwigComponentBlockLookupElement(@NotNull String blockName, @Nullable String typeText) {
        this.blockName = blockName;
        this.typeText = typeText;
    }

    @Override
    public @NotNull String getLookupString() {
        return "twig:block name=\"" + blockName + "\"";
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(Symfony2Icons.SYMFONY);

        if (typeText != null) {
            presentation.setTypeText(typeText);
            presentation.setTypeGrayed(true);
        }
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        Editor editor = context.getEditor();
        Document document = editor.getDocument();

        int startOffset = context.getStartOffset();
        int tailOffset = context.getTailOffset();

        // Find the actual start of the tag - the '<' might be a separate token
        int actualStartOffset = startOffset;
        if (startOffset > 0) {
            CharSequence chars = document.getCharsSequence();
            if (chars.charAt(startOffset - 1) == '<') {
                actualStartOffset = startOffset - 1;
            }
        }

        // Build the full block tag with closing tag
        String fullTag = "<twig:block name=\"" + blockName + "\"></twig:block>";

        // Delete any text the user typed (from actual start to tail offset)
        if (tailOffset > actualStartOffset) {
            document.deleteString(actualStartOffset, tailOffset);
        }

        // Insert the full block tag
        document.insertString(actualStartOffset, fullTag);

        // Move caret between the opening and closing tags
        int caretOffset = actualStartOffset + fullTag.length() - "</twig:block>".length();
        editor.getCaretModel().moveToOffset(caretOffset);
    }
}
