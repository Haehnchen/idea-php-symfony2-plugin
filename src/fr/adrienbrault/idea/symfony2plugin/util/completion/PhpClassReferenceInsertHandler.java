package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import org.jetbrains.annotations.NotNull;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.intellij.codeInsight.completion.InsertHandler;

/**
* simplified PhpReferenceInsertHandler which not use trailing quote
*/
public class PhpClassReferenceInsertHandler implements InsertHandler<LookupElement> {

    private static final PhpClassReferenceInsertHandler instance = new PhpClassReferenceInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        Object object = lookupElement.getObject();

        if (!(object instanceof PhpClass)) {
            return;
        }

        StringBuilder textToInsertBuilder = new StringBuilder();
        PhpClass aClass = (PhpClass)object;
        String fqn = aClass.getNamespaceName();

        if(fqn.startsWith("\\")) {
            fqn = fqn.substring(1);
        }

        textToInsertBuilder.append(fqn);
        context.getDocument().insertString(context.getStartOffset(), textToInsertBuilder);

    }

    public static PhpClassReferenceInsertHandler getInstance(){
        return instance;
    }

}
