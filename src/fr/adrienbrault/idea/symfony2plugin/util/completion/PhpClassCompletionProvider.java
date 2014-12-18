package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

public class PhpClassCompletionProvider extends CompletionProvider<CompletionParameters> {


    private final boolean withInterface;

    public PhpClassCompletionProvider() {
        this(false);
    }

    public PhpClassCompletionProvider(boolean withInterface) {
        this.withInterface = withInterface;
    }

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet resultSet) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        addClassCompletion(parameters, resultSet, psiElement, withInterface);

    }

    public static void addClassCompletion(CompletionParameters parameters, final CompletionResultSet resultSet, PsiElement psiElement, boolean withInterface) {

        // Foo\|Bar
        // Foo|\Bar
        PhpElementsUtil.visitNamespaceClassForCompletion(psiElement, parameters.getOffset(), new PhpElementsUtil.ClassForCompletionVisitor() {
            @Override
            public void visit(PhpClass phpClass, String presentableFQN, String prefix) {
                resultSet.addElement(LookupElementBuilder.create(prefix + presentableFQN).withIcon(phpClass.getIcon()));
            }
        });

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {

            for(PhpClass phpClass: phpIndex.getClassesByName(className)) {
                resultSet.addElement(new MyPhpLookupElement(phpClass).withInsertHandler(PhpReferenceTrimBackslashInsertHandler.getInstance()));
            }

            if(withInterface) {
                for(PhpClass phpClass: phpIndex.getInterfacesByName(className)) {
                    resultSet.addElement(new MyPhpLookupElement(phpClass).withInsertHandler(PhpReferenceTrimBackslashInsertHandler.getInstance()));
                }
            }

        }

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
