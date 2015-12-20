package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOptionEnum;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class FormOptionGotoCompletionRegistrar implements GotoCompletionRegistrar {

    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE), new FormOptionBuilderCompletionContributor());
    }

    private static class FormOptionBuilderCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.getParent());
            if(arrayCreationExpression != null) {
                PsiElement parameterList = arrayCreationExpression.getParent();
                if (parameterList instanceof ParameterList) {
                    PsiElement context = parameterList.getContext();
                    if(context instanceof MethodReference) {
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreationExpression);
                        if(currentIndex != null && currentIndex.getIndex() == 2) {
                            if (new Symfony2InterfacesUtil().isFormBuilderFormTypeCall(context)) {
                                return getMatchingOption((ParameterList) parameterList, psiElement);
                            }
                        }
                    }
                }
            }

            return null;

        }

        @Nullable
        private GotoCompletionProvider getMatchingOption(ParameterList parameterList, @NotNull PsiElement psiElement) {

            // form name can be a string alias; also resolve on constants, properties, ...
            PsiElement psiElementAt = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, 1);

            String formTypeName = null;
            if(psiElementAt != null) {
                PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(psiElementAt);
                if(phpClass != null) {
                    formTypeName = phpClass.getFQN();
                }
            }

            // fallback to form
            if(formTypeName == null) {
                formTypeName = "form";
            }

            return new FormReferenceCompletionProvider(psiElement, formTypeName);
        }

    }

    private static class FormReferenceCompletionProvider extends GotoCompletionProvider {

        private final String formType;

        public FormReferenceCompletionProvider(@NotNull PsiElement element, @NotNull String formType) {
            super(element);
            this.formType = formType;
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            final String value = ((StringLiteralExpression) element).getContents();
            if(StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }

            final Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            FormOptionsUtil.visitFormOptions(getProject(), formType, new FormOptionVisitor() {
                @Override
                public void visit(@NotNull PsiElement psiElement, @NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum) {
                    if (option.equals(value)) {
                        psiElements.add(psiElement);
                    }
                }
            });

            return psiElements;
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();
            FormOptionsUtil.visitFormOptions(getProject(), formType, new FormOptionLookupVisitor(lookupElements));
            return lookupElements;
        }

    }
}
