package fr.adrienbrault.idea.symfonyplugin.templating.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfonyplugin.asset.AssetLookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAssetFunctionInsertHandler implements InsertHandler<AssetLookupElement> {

    private static final TwigAssetFunctionInsertHandler instance = new TwigAssetFunctionInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull AssetLookupElement lookupElement) {
        context.getDocument().insertString(context.getStartOffset(), "{{ asset('");
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "') }}");
    }

    public static TwigAssetFunctionInsertHandler getInstance(){
        return instance;
    }

}