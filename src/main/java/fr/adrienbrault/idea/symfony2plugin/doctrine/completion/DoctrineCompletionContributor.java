package fr.adrienbrault.idea.symfony2plugin.doctrine.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.ClassConstantLookupElementAbstract;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineCompletionContributor extends CompletionContributor {

    public DoctrineCompletionContributor() {

        // getRepository(FOO) -> getRepository(FOO::class)
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(ConstantReference.class), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodReference methodReference = PhpElementsUtil.findMethodReferenceOnClassConstant(psiElement);
                if (methodReference == null) {
                    return;
                }

                if (!(
                    PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Common\\Persistence\\ObjectManager", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Persistence\\ObjectManager", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Persistence\\ManagerRegistry", "getRepository")
                )) {
                    return;
                }

                Collection<DoctrineModel> modelClasses = EntityHelper.getModelClasses(psiElement.getProject());

                for (DoctrineModel doctrineModel : modelClasses) {
                    PhpClass phpClass = doctrineModel.getPhpClass();
                    if (phpClass.isAbstract() || phpClass.isInterface()) {
                        continue;
                    }

                    LookupElement elementBuilder = new Foo(phpClass);

                    // does this have an effect really?
                    completionResultSet.addElement(
                        PrioritizedLookupElement.withPriority(elementBuilder, 100)
                    );
                }

            }

        });
    }

    private static class Foo extends ClassConstantLookupElementAbstract {

        public Foo(@NotNull PhpClass phpClass) {
            super(phpClass);
        }

        @Override
        public void renderElement(LookupElementPresentation presentation) {
            super.renderElement(presentation);
            presentation.setIcon(Symfony2Icons.DOCTRINE);
        }
    }
}
