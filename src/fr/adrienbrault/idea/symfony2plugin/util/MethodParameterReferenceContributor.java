package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantPsiReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.DefaultReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.DefaultReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

public class MethodParameterReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {

                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    ArrayList<MethodParameterSetting> configs = new ArrayList<MethodParameterSetting>();

                    configs.addAll(AssistantReferenceUtil.getMethodsParameterSettings(psiElement.getProject()));
                    configs.addAll(getInternalMethodParameterSetting());

                    ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
                    if (parameterList == null) {
                        return new PsiReference[0];
                    }

                    if(!(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();
                    Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();

                    ArrayList<PsiReference> psiReferences = new ArrayList<PsiReference>();
                    for (MethodParameterSetting config: configs) {

                        if(!interfacesUtil.isCallTo(method, config.getCallTo(), config.getMethodName())) {
                            continue;
                        }

                        AssistantReferenceContributor referenceContributor = AssistantReferenceUtil.getContributor(config);
                        if(referenceContributor != null && referenceContributor.isContributedElement(psiElement, config)) {
                            Collections.addAll(psiReferences, AssistantReferenceUtil.getPsiReference(config, (StringLiteralExpression) psiElement));
                        }

                    }


                    return psiReferences.toArray(new PsiReference[psiReferences.size()]);
                }


                private ArrayList<MethodParameterSetting> getInternalMethodParameterSetting() {
                    ArrayList<MethodParameterSetting> methodParameterSettings = new ArrayList<MethodParameterSetting>();

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface",
                        "setDefaults",
                        0,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.TRANSLATION_DOMAIN,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "translation_domain"
                    ));

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface",
                        "setDefaults",
                        0,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.TRANSLATION_DOMAIN,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "data_class"
                    ).withPsiReference(new AssistantPsiReferenceContributor() {
                        @Override
                        public PsiReference[] getPsiReferences(StringLiteralExpression psiElement) {
                            return new PsiReference[]{ new EntityReference(psiElement, true)};
                        }
                    }));

                    return methodParameterSettings;
                }

            }

        );

    }


}
