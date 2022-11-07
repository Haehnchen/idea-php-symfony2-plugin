package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.DefaultReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.DefaultReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodParameterReferenceContributorExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodParameterReferenceContributorParameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodParameterReferenceContributor extends PsiReferenceContributor {

    private static final ExtensionPointName<MethodParameterReferenceContributorExtension> EXTENSIONS = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.MethodParameterReferenceContributorExtension");

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    List<MethodParameterSetting> configs = new ArrayList<>();

                    configs.addAll(AssistantReferenceUtil.getMethodsParameterSettings(psiElement.getProject()));
                    configs.addAll(getInternalMethodParameterSetting());
                    configs.addAll(getExtensionMethodParameterSetting(psiElement.getProject()));

                    ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
                    if (parameterList == null) {
                        return new PsiReference[0];
                    }

                    if(!(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();

                    List<PsiReference> psiReferences = new ArrayList<>();

                    // get config in method scope; so we can pipe them
                    ArrayList<MethodParameterSetting> methodScopeConfigs = new ArrayList<>();
                    for (MethodParameterSetting config: configs) {


                        // @TODO: fake MethodMatcher call; replace this :)
                        // we need nearest parameter value
                        PsiElement psiNearestParameter = PsiElementUtils.getParentOfTypeFirstChild(psiElement, ParameterList.class);
                        if(psiNearestParameter == null) {
                            continue;
                        }

                        MethodMatcher.MethodMatchParameter matchParameter = MethodMatcher.getMatchedSignatureWithDepth(psiNearestParameter, new MethodMatcher.CallToSignature[]{new MethodMatcher.CallToSignature(config.getCallTo(), config.getMethodName())}, config.getIndexParameter());
                        if(matchParameter != null) {
                            AssistantReferenceContributor referenceContributor = AssistantReferenceUtil.getContributor(config);
                            if(referenceContributor != null && referenceContributor.isContributedElement(psiElement, config)) {
                                Collections.addAll(psiReferences, AssistantReferenceUtil.getPsiReference(config, (StringLiteralExpression) psiElement, methodScopeConfigs, method));
                            }
                        }

                    }

                    return psiReferences.toArray(new PsiReference[0]);
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }

                private Collection<MethodParameterSetting> getInternalMethodParameterSetting() {
                    Collection<MethodParameterSetting> methodParameterSettings = new ArrayList<>();

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
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.CLASS,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "data_class"
                    ));

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\OptionsResolver\\OptionsResolver",
                        "setDefaults",
                        0,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.TRANSLATION_DOMAIN,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "translation_domain"
                    ));

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\OptionsResolver\\OptionsResolver",
                        "setDefaults",
                        0,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.CLASS,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "data_class"
                    ));

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\Form\\FormBuilderInterface",
                        "add",
                        2,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.FORM_TYPE,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE,
                        "type"
                    ));

                    methodParameterSettings.add(new MethodParameterSetting(
                        "\\Symfony\\Component\\Form\\FormFactoryInterface",
                        "create",
                        0,
                        DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM.FORM_TYPE,
                        DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM.PARAMETER,
                        null
                    ));

                    return methodParameterSettings;
                }

                private Collection<MethodParameterSetting> getExtensionMethodParameterSetting(Project project) {

                    Collection<MethodParameterSetting> methodParameterSettings = new ArrayList<>();

                    MethodParameterReferenceContributorExtension[] extensions = EXTENSIONS.getExtensions();
                    if(extensions.length == 0) {
                        return methodParameterSettings;
                    }

                    MethodParameterReferenceContributorParameter parameter = new MethodParameterReferenceContributorParameter(project);
                    for(MethodParameterReferenceContributorExtension extension: extensions) {
                        methodParameterSettings.addAll(extension.getSettings(parameter));
                    }

                    return methodParameterSettings;
                }

            }

        );

    }


}
