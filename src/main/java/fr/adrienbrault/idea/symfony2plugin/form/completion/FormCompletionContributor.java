package fr.adrienbrault.idea.symfony2plugin.form.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormCompletionContributor extends CompletionContributor {

    public FormCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(ConstantReference.class), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodReference methodReference = PhpElementsUtil.findMethodReferenceOnClassConstant(psiElement);
                if(methodReference == null) {
                    return;
                }

                if(!(
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Form\\FormBuilderInterface", "add") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Form\\FormBuilderInterface", "create") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Form\\FormFactoryInterface", "createNamedBuilder") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Form\\FormFactoryInterface", "createNamed")
                )) {
                    return;
                }

                for (PhpClass phpClass : PhpIndex.getInstance(psiElement.getProject()).getAllSubclasses("\\Symfony\\Component\\Form\\FormTypeInterface")) {
                    if(phpClass.isAbstract() || phpClass.isInterface()) {
                        continue;
                    }

                    LookupElement elementBuilder = new FormClassConstantsLookupElement(phpClass);

                    // does this have an effect really?
                    completionResultSet.addElement(
                        PrioritizedLookupElement.withExplicitProximity(PrioritizedLookupElement.withPriority(elementBuilder, 1000), 1000)
                    );
                }

            }

        });
    }

}
