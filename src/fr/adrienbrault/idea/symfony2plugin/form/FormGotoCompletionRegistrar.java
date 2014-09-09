package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class FormGotoCompletionRegistrar implements GotoCompletionRegistrar {

    public void register(GotoCompletionRegistrarParameter registrar) {

        // FormBuilderInterface:add("", "type")
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement parent = psiElement.getParent();
                if(parent == null) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(parent, 1)
                    .withSignature(Symfony2InterfacesUtil.getFormBuilderInterface())
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return new FormBuilderAddGotoCompletionProvider(parent);
            }
        });

    }

    private static class FormBuilderAddGotoCompletionProvider extends GotoCompletionProvider {

        public FormBuilderAddGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return FormUtil.getFormTypeLookupElements(getElement().getProject());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            PhpClass formTypeToClass = FormUtil.getFormTypeToClass(getElement().getProject(), ((StringLiteralExpression) element).getContents());
            if(formTypeToClass == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(new PsiElement[] { formTypeToClass });
        }
    }

}
