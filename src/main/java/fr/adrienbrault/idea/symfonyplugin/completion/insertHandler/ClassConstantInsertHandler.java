package fr.adrienbrault.idea.symfonyplugin.completion.insertHandler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassConstantInsertHandler implements InsertHandler<LookupElement> {

    private static final ClassConstantInsertHandler instance = new ClassConstantInsertHandler();

    private ClassConstantInsertHandler() {}

    @Override
    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        if(!(lookupElement instanceof ClassConstantLookupElementInterface) || !(lookupElement.getObject() instanceof PhpClass)) {
            return;
        }

        PhpReferenceInsertHandler.getInstance().handleInsert(context, lookupElement);
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "::class");
    }

    public static ClassConstantInsertHandler getInstance(){
        return instance;
    }

    public interface ClassConstantLookupElementInterface {
        @NotNull
        PhpClass getPhpClass();
    }
}
