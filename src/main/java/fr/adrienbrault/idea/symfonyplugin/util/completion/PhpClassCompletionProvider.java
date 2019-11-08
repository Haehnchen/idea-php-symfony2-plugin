package fr.adrienbrault.idea.symfonyplugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassCompletionProvider extends CompletionProvider<CompletionParameters> {

    private final boolean withInterface;
    private boolean withLeadingBackslash = false;

    public PhpClassCompletionProvider() {
        this(false);
    }

    public PhpClassCompletionProvider(boolean withInterface) {
        this.withInterface = withInterface;
    }

    public PhpClassCompletionProvider withTrimLeadBackslash(boolean withLeadingBackslash) {
        this.withLeadingBackslash = withLeadingBackslash;
        return this;
    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet resultSet) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        addClassCompletion(parameters, resultSet, psiElement, withInterface, withLeadingBackslash);

    }
    public static void addClassCompletion(CompletionParameters parameters, final CompletionResultSet resultSet, PsiElement psiElement, boolean withInterface) {
        addClassCompletion(parameters, resultSet, psiElement, withInterface, false);
    }

    public static void addClassCompletion(CompletionParameters parameters, final CompletionResultSet resultSet, PsiElement psiElement, boolean withInterface, boolean withLeadBackslash) {

        // Foo\|Bar
        // Foo|\Bar
        PhpElementsUtil.visitNamespaceClassForCompletion(psiElement, parameters.getOffset(), (phpClass, presentableFQN, prefix) ->
            resultSet.addElement(LookupElementBuilder.create(prefix + presentableFQN).withIcon(phpClass.getIcon()))
        );

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {

            for(PhpClass phpClass: phpIndex.getClassesByName(className)) {
                resultSet.addElement(
                    wrapInsertHandler(new MyPhpLookupElement(phpClass), withLeadBackslash)
                );
            }

            if(withInterface) {
                for(PhpClass phpClass: phpIndex.getInterfacesByName(className)) {
                    resultSet.addElement(
                        wrapInsertHandler(new MyPhpLookupElement(phpClass), withLeadBackslash)
                    );
                }
            }

        }

    }

    private static MyPhpLookupElement wrapInsertHandler(MyPhpLookupElement lookupElement, boolean withLeadBackslash) {

        if(withLeadBackslash) {
            return lookupElement.withInsertHandler(PhpReferenceInsertHandler.getInstance());
        }

        return lookupElement.withInsertHandler(PhpReferenceTrimBackslashInsertHandler.getInstance());
    }

    private static class MyPhpLookupElement extends PhpLookupElement {

        public MyPhpLookupElement(@NotNull PhpNamedElement namedElement) {
            super(namedElement);
        }

        public MyPhpLookupElement withInsertHandler(InsertHandler insertHandler) {
            this.handler = insertHandler;
            return this;
        }

    }

}
