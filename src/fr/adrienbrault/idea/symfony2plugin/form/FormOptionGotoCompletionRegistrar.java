package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
            String formTypeName = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(parameterList, 1));

            // formtype is not a string, so try to find php class types
            if(formTypeName == null) {
                PsiElement psiElement1 = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, 1);
                if(psiElement1 instanceof PhpTypedElementImpl) {
                    formTypeName = ((PhpTypedElementImpl) psiElement1).getType().toString();
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

        @Nullable
        private final String formType;

        public FormReferenceCompletionProvider(@NotNull PsiElement element, @Nullable String formType) {
            super(element);
            this.formType = formType;
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String value = ((StringLiteralExpression) element).getContents();
            if(StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }

            Set<String> classNames = new HashSet<String>();

            Map<String, String> defaultOptions = FormOptionsUtil.getFormDefaultKeys(element.getProject(), formType);
            if(defaultOptions.containsKey(value)) {
                classNames.add(defaultOptions.get(value));
            }

            Map<String, FormOption> formExtension = FormOptionsUtil.getFormExtensionKeys(getProject(), "form", this.formType);
            if(formExtension.containsKey(value)) {
                classNames.add(formExtension.get(value).getFormClass().getPhpClass().getPresentableFQN());
            }

            Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            for(String className: classNames) {
                Method method = PhpElementsUtil.getClassMethod(getProject(), className, "setDefaultOptions");
                if(method != null) {
                    PsiElement keyValue = PhpElementsUtil.findArrayKeyValueInsideReference(method, "setDefaults", value);
                    if(keyValue != null) {
                        psiElements.add(keyValue);
                    }
                }
            }

            return psiElements;
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

            for(FormOption formOption: FormOptionsUtil.getFormExtensionKeys(getProject(), "form", this.formType).values()) {
                lookupElements.add(FormOptionsUtil.getOptionLookupElement(formOption));
            }

            for(Map.Entry<String, String> extension: FormOptionsUtil.getFormDefaultKeys(getProject(), this.formType).entrySet()) {
                String typeText = extension.getValue();
                if(typeText.lastIndexOf("\\") != -1) {
                    typeText = typeText.substring(typeText.lastIndexOf("\\") + 1);
                }

                if(typeText.endsWith("Type")) {
                    typeText = typeText.substring(0, typeText.length() - 4);
                }

                lookupElements.add(LookupElementBuilder.create(extension.getKey())
                    .withTypeText(typeText, true)
                    .withIcon(Symfony2Icons.FORM_OPTION)
                );

            }

            return lookupElements;
        }

    }
}
