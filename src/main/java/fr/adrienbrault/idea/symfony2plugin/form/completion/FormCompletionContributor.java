package fr.adrienbrault.idea.symfony2plugin.form.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormCompletionContributor extends CompletionContributor {

    public FormCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(ConstantReference.class), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                // $builder->add('field', Type::class), $factory->create(Type::class), $this->createForm(type: Type::class)
                ConstantReference argument = (ConstantReference) completionParameters.getPosition().getParent();
                if (!(argument.getParent() instanceof ParameterList parameters)
                    || !(parameters.getParent() instanceof MethodReference method)
                    || FormUtil.getFormTypeArgument(method) != argument) {
                    return;
                }

                for (FormTypeClass formType : FormUtil.getFormTypeClasses(psiElement.getProject()).values()) {
                    PhpClass phpClass = formType.getPhpClass(psiElement.getProject());
                    if (phpClass == null || phpClass.isAbstract() || phpClass.isInterface() || phpClass.isTrait() || phpClass.isEnum()) {
                        continue;
                    }

                    LookupElement elementBuilder = new FormClassConstantsLookupElement(phpClass, formType.getType());

                    completionResultSet.addElement(
                        PrioritizedLookupElement.withPriority(elementBuilder, 100)
                    );
                }
            }
        });
    }

}
