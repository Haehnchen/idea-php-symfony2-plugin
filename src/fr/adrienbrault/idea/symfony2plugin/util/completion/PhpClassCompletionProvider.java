package fr.adrienbrault.idea.symfony2plugin.util.completion;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

public class PhpClassCompletionProvider  extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(parameters.getOriginalFile().getProject());

        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {
            resultSet.addElement(new PhpLookupElement(className, PhpClassIndex.KEY, parameters.getOriginalFile().getProject(), PhpClassReferenceInsertHandler.getInstance()));
        }
    }

}
